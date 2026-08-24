package student.imports


import com.example.academics.repos.setTenantSchema
import com.example.student.dtos.requests.CreateStudentRequest
import com.example.student.dtos.requests.CreateUserPart

import com.example.student.services.StudentService
import com.example.student.tables.NewGradeClassTable
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.ByteArrayInputStream

object ImportStudentsFromExcel {

    private const val FULL_NAME_HEADER =
        "fullname"

    private const val CURRENT_CLASS_HEADER =
        "currentclass"

    private const val FATHER_CONTACT_HEADER =
        "contactoffather"

    private const val MOTHER_CONTACT_HEADER =
        "contactofmother"

    private const val DISCOUNTED_HEADER =
        "isdiscountedstudent"

    fun run(
        tenantSchema: String,
        fileBytes: ByteArray,
        originalFileName: String,
        sendAdmissionSms: Boolean = false,
        admissionSmsMessage: String? = null
    ): StudentImportResult {

        println()
        println("======================================================")
        println("[EXCEL IMPORT] Import started")
        println("[EXCEL IMPORT] tenantSchema=$tenantSchema")
        println("[EXCEL IMPORT] originalFileName=$originalFileName")
        println("[EXCEL IMPORT] uploadedFileSize=${fileBytes.size} bytes")
        println("[EXCEL IMPORT] sendAdmissionSms=$sendAdmissionSms")
        println(
            "[EXCEL IMPORT] admissionSmsMessagePresent=${
                !admissionSmsMessage.isNullOrBlank()
            }"
        )
        println("======================================================")

        require(
            tenantSchema.isNotBlank()
        ) {
            "Tenant schema is required."
        }

        require(
            fileBytes.isNotEmpty()
        ) {
            "The uploaded Excel file is empty."
        }

        require(
            originalFileName
                .lowercase()
                .endsWith(".xlsx")
        ) {
            "Only Excel .xlsx files are allowed."
        }

        if (sendAdmissionSms) {
            require(
                !admissionSmsMessage.isNullOrBlank()
            ) {
                "Admission SMS message is required when Admission SMS is enabled."
            }
        }

        val classMap =
            loadClassMap(
                tenantSchema = tenantSchema
            )

        println(
            "[EXCEL IMPORT] Available classes=${classMap.keys}"
        )

        val errors =
            mutableListOf<StudentImportError>()

        var importedCount =
            0

        var failedCount =
            0

        ByteArrayInputStream(
            fileBytes
        ).use { inputStream ->

            WorkbookFactory.create(
                inputStream
            ).use { workbook ->

                require(
                    workbook.numberOfSheets > 0
                ) {
                    "The uploaded Excel workbook has no worksheets."
                }

                val sheet =
                    workbook.getSheetAt(0)

                println(
                    "[EXCEL IMPORT] Sheet name=${sheet.sheetName}"
                )

                println(
                    "[EXCEL IMPORT] First row=${sheet.firstRowNum}"
                )

                println(
                    "[EXCEL IMPORT] Last row=${sheet.lastRowNum}"
                )

                val headerRow =
                    sheet.getRow(
                        sheet.firstRowNum
                    )
                        ?: throw IllegalArgumentException(
                            "The Excel file does not contain a header row."
                        )

                val headerIndexes =
                    buildHeaderIndex(
                        headerRow = headerRow
                    )

                println(
                    "[EXCEL IMPORT] Resolved headers=$headerIndexes"
                )

                validateRequiredHeaders(
                    headerIndexes = headerIndexes
                )

                val formatter =
                    DataFormatter()

                for (
                rowIndex in
                (sheet.firstRowNum + 1)..sheet.lastRowNum
                ) {
                    val row =
                        sheet.getRow(
                            rowIndex
                        )
                            ?: continue

                    val excelRowNumber =
                        rowIndex + 1

                    if (
                        isRowEmpty(
                            row = row,
                            formatter = formatter
                        )
                    ) {
                        println(
                            "[EXCEL IMPORT] Skipping empty row=$excelRowNumber"
                        )

                        continue
                    }

                    println()
                    println(
                        "[EXCEL IMPORT] Processing row=$excelRowNumber"
                    )

                    try {
                        val fullName =
                            readCell(
                                row = row,
                                columnIndex =
                                    headerIndexes.getValue(
                                        FULL_NAME_HEADER
                                    ),
                                formatter = formatter
                            )

                        val currentClass =
                            readCell(
                                row = row,
                                columnIndex =
                                    headerIndexes.getValue(
                                        CURRENT_CLASS_HEADER
                                    ),
                                formatter = formatter
                            )

                        val contactOfFather =
                            readCell(
                                row = row,
                                columnIndex =
                                    headerIndexes.getValue(
                                        FATHER_CONTACT_HEADER
                                    ),
                                formatter = formatter
                            )

                        val contactOfMother =
                            headerIndexes[
                                MOTHER_CONTACT_HEADER
                            ]
                                ?.let { columnIndex ->
                                    readCell(
                                        row = row,
                                        columnIndex = columnIndex,
                                        formatter = formatter
                                    )
                                }
                                ?.takeIf { value ->
                                    value.isNotBlank()
                                }

                        val discountedText =
                            headerIndexes[
                                DISCOUNTED_HEADER
                            ]
                                ?.let { columnIndex ->
                                    readCell(
                                        row = row,
                                        columnIndex = columnIndex,
                                        formatter = formatter
                                    )
                                }
                                .orEmpty()

                        val normalizedClassName =
                            normalizeClassName(
                                currentClass
                            )

                        val isDiscounted =
                            parseBoolean(
                                discountedText
                            )

                        println(
                            "[EXCEL IMPORT] Parsed row=$excelRowNumber, " +
                                    "fullName=$fullName, " +
                                    "currentClass=$currentClass, " +
                                    "fatherContact=$contactOfFather, " +
                                    "motherContact=$contactOfMother, " +
                                    "isDiscounted=$isDiscounted"
                        )

                        require(
                            fullName.isNotBlank()
                        ) {
                            "Student name is required."
                        }

                        require(
                            currentClass.isNotBlank()
                        ) {
                            "Current class is required for $fullName."
                        }

                        require(
                            contactOfFather.isNotBlank()
                        ) {
                            "Father's contact is required for $fullName."
                        }

                        val classId =
                            classMap[
                                normalizedClassName
                            ]
                                ?: throw IllegalArgumentException(
                                    "Class '$currentClass' was not found. " +
                                            "Available classes: ${
                                                classMap.keys.joinToString(", ")
                                            }"
                                )

                        val request =
                            CreateStudentRequest(
                                user =
                                    CreateUserPart(
                                        fullName = fullName,
                                        role = "student"
                                    ),

                                currentNewGradeClassId =
                                    classId,

                                family =
                                    null,

                                isDiscountedStudent =
                                    isDiscounted,

                                contactOfFather =
                                    contactOfFather,

                                contactOfMother =
                                    contactOfMother,

                                sendAdmissionSms =
                                    sendAdmissionSms,

                                admissionSmsMessage =
                                    if (sendAdmissionSms) {
                                        admissionSmsMessage
                                            ?.trim()
                                    } else {
                                        null
                                    }
                            )

                        println(
                            "[EXCEL IMPORT] Calling StudentService.createStudent for row=$excelRowNumber"
                        )

                        val createdStudent =
                            StudentService.createStudent(
                                tenantSchema = tenantSchema,
                                request = request
                            )

                        importedCount += 1

                        println(
                            "[EXCEL IMPORT] SUCCESS row=$excelRowNumber, " +
                                    "student=$fullName, " +
                                    "classId=$classId, " +
                                    "admissionSms=$sendAdmissionSms"
                        )

                        println(
                            "[EXCEL IMPORT] Created student=$createdStudent"
                        )

                    } catch (exception: Exception) {
                        failedCount += 1

                        val errorMessage =
                            exception.message
                                ?: "Unable to import this student."

                        errors.add(
                            StudentImportError(
                                rowNumber =
                                    excelRowNumber,

                                message =
                                    errorMessage
                            )
                        )

                        println(
                            "[EXCEL IMPORT] FAILED row=$excelRowNumber"
                        )

                        println(
                            "[EXCEL IMPORT] Error type=${exception::class.qualifiedName}"
                        )

                        println(
                            "[EXCEL IMPORT] Error message=$errorMessage"
                        )

                        exception.printStackTrace()
                    }
                }
            }
        }

        println()
        println("======================================================")
        println("[EXCEL IMPORT] Import completed")
        println("[EXCEL IMPORT] originalFileName=$originalFileName")
        println("[EXCEL IMPORT] uploadedFileSize=${fileBytes.size}")
        println("[EXCEL IMPORT] importedCount=$importedCount")
        println("[EXCEL IMPORT] failedCount=$failedCount")
        println("[EXCEL IMPORT] sendAdmissionSms=$sendAdmissionSms")
        println("======================================================")

        return StudentImportResult(
            importedCount = importedCount,
            failedCount = failedCount,
            message =
                if (failedCount == 0) {
                    "All students were imported successfully."
                } else {
                    "Import completed with some failed rows."
                },
            errors = errors
        )
    }

    private fun loadClassMap(
        tenantSchema: String
    ): Map<String, Int> {

        return transaction {

            setTenantSchema(
                tenantSchema
            )

            NewGradeClassTable
                .selectAll()
                .associate { row ->

                    val normalizedClassName =
                        normalizeClassName(
                            row[
                                NewGradeClassTable.name
                            ]
                        )

                    val classId =
                        row[
                            NewGradeClassTable.id
                        ].value

                    normalizedClassName to classId
                }
        }
    }

    private fun buildHeaderIndex(
        headerRow: org.apache.poi.ss.usermodel.Row
    ): Map<String, Int> {

        val headers =
            mutableMapOf<String, Int>()

        for (cell in headerRow) {
            val normalizedHeader =
                normalizeHeader(
                    cell.toString()
                )

            if (normalizedHeader.isNotBlank()) {
                headers[
                    normalizedHeader
                ] = cell.columnIndex
            }
        }

        return headers
    }

    private fun validateRequiredHeaders(
        headerIndexes: Map<String, Int>
    ) {

        val requiredHeaders =
            listOf(
                FULL_NAME_HEADER,
                CURRENT_CLASS_HEADER,
                FATHER_CONTACT_HEADER
            )

        val missingHeaders =
            requiredHeaders.filter { header ->
                !headerIndexes.containsKey(
                    header
                )
            }

        require(
            missingHeaders.isEmpty()
        ) {
            "Missing required Excel headers: ${
                missingHeaders.joinToString(", ")
            }. Required headers are: fullName, currentClass, contactOfFather."
        }
    }

    private fun readCell(
        row: org.apache.poi.ss.usermodel.Row,
        columnIndex: Int,
        formatter: DataFormatter
    ): String {

        val cell =
            row.getCell(
                columnIndex,
                org.apache.poi.ss.usermodel.Row.MissingCellPolicy.RETURN_BLANK_AS_NULL
            )
                ?: return ""

        return when (cell.cellType) {
            CellType.FORMULA -> {
                formatter
                    .formatCellValue(cell)
                    .trim()
            }

            else -> {
                formatter
                    .formatCellValue(cell)
                    .trim()
            }
        }
    }

    private fun isRowEmpty(
        row: org.apache.poi.ss.usermodel.Row,
        formatter: DataFormatter
    ): Boolean {

        if (
            row.firstCellNum < 0 ||
            row.lastCellNum < 0
        ) {
            return true
        }

        for (
        columnIndex in
        row.firstCellNum until row.lastCellNum
        ) {
            val cell =
                row.getCell(
                    columnIndex,
                    org.apache.poi.ss.usermodel.Row.MissingCellPolicy.RETURN_BLANK_AS_NULL
                )

            if (
                cell != null &&
                formatter
                    .formatCellValue(cell)
                    .trim()
                    .isNotBlank()
            ) {
                return false
            }
        }

        return true
    }

    private fun normalizeHeader(
        value: String
    ): String {

        return value
            .trim()
            .lowercase()
            .replace(
                Regex("[^a-z0-9]"),
                ""
            )
    }

    private fun normalizeClassName(
        value: String
    ): String {

        return value
            .trim()
            .lowercase()
            .replace(
                Regex("\\s+"),
                " "
            )
    }

    private fun parseBoolean(
        value: String
    ): Boolean {

        return when (
            value
                .trim()
                .lowercase()
        ) {
            "yes",
            "true",
            "1",
            "y" -> true

            else -> false
        }
    }
}