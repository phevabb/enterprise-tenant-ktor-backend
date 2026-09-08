package com.example.commands


import com.example.academics.repos.setTenantSchema
import com.example.config.DatabaseFactory
import com.example.student.dtos.requests.CreateUserPart
import com.example.student.services.StudentService
import java.io.File
import com.example.student.dtos.requests.CreateStudentRequest
import com.example.student.tables.NewGradeClassTable
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction




import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import student.imports.StudentImportError
import student.imports.StudentImportResult
import java.io.ByteArrayInputStream
//
//object ImportStudentsFromExcel {
//
//    private const val FULL_NAME_HEADER =
//        "fullname"
//
//    private const val CURRENT_CLASS_HEADER =
//        "currentclass"
//
//    private const val FATHER_CONTACT_HEADER =
//        "contactoffather"
//
//    private const val MOTHER_CONTACT_HEADER =
//        "contactofmother"
//
//    private const val DISCOUNTED_HEADER =
//        "isdiscountedstudent"
//
//    fun run(
//        tenantSchema: String,
//        fileBytes: ByteArray,
//        originalFileName: String,
//        sendAdmissionSms: Boolean = false,
//        admissionSmsMessage: String? = null
//    ): StudentImportResult {
//
//        println()
//        println("======================================================")
//        println("[EXCEL IMPORT] IMPORT STARTED")
//        println("[EXCEL IMPORT] tenantSchema=$tenantSchema")
//        println("[EXCEL IMPORT] originalFileName=$originalFileName")
//        println("[EXCEL IMPORT] uploadedFileSize=${fileBytes.size}")
//        println("[EXCEL IMPORT] sendAdmissionSms=$sendAdmissionSms")
//        println(
//            "[EXCEL IMPORT] admissionSmsMessagePresent=${
//                !admissionSmsMessage.isNullOrBlank()
//            }"
//        )
//        println("======================================================")
//
//        require(
//            tenantSchema.isNotBlank()
//        ) {
//            "Tenant schema is required."
//        }
//
//        require(
//            fileBytes.isNotEmpty()
//        ) {
//            "The uploaded Excel file is empty."
//        }
//
//        require(
//            originalFileName
//                .trim()
//                .lowercase()
//                .endsWith(".xlsx")
//        ) {
//            "Only Excel .xlsx files are allowed."
//        }
//
//        if (sendAdmissionSms) {
//            require(
//                !admissionSmsMessage.isNullOrBlank()
//            ) {
//                "Admission SMS message is required when Admission SMS is enabled."
//            }
//        }
//
//        val classMap =
//            loadClassMap(
//                tenantSchema = tenantSchema
//            )
//
//        println(
//            "[EXCEL IMPORT] Available classes=${classMap.keys}"
//        )
//
//        val errors =
//            mutableListOf<StudentImportError>()
//
//        var importedCount =
//            0
//
//        var failedCount =
//            0
//
//        val formatter =
//            DataFormatter()
//
//        ByteArrayInputStream(
//            fileBytes
//        ).use { inputStream ->
//
//            WorkbookFactory.create(
//                inputStream
//            ).use { workbook ->
//
//                require(
//                    workbook.numberOfSheets > 0
//                ) {
//                    "The uploaded Excel workbook contains no worksheets."
//                }
//
//                val sheet =
//                    workbook.getSheetAt(0)
//
//                println(
//                    "[EXCEL IMPORT] Worksheet=${sheet.sheetName}"
//                )
//
//                println(
//                    "[EXCEL IMPORT] First row index=${sheet.firstRowNum}"
//                )
//
//                println(
//                    "[EXCEL IMPORT] Last row index=${sheet.lastRowNum}"
//                )
//
//                val headerRow =
//                    sheet.getRow(
//                        sheet.firstRowNum
//                    )
//                        ?: throw IllegalArgumentException(
//                            "The uploaded Excel file has no header row."
//                        )
//
//                val headerIndexes =
//                    buildHeaderIndexes(
//                        headerRow = headerRow,
//                        formatter = formatter
//                    )
//
//                println(
//                    "[EXCEL IMPORT] Headers=$headerIndexes"
//                )
//
//                validateHeaders(
//                    headerIndexes = headerIndexes
//                )
//
//                for (
//                rowIndex in
//                (sheet.firstRowNum + 1)..sheet.lastRowNum
//                ) {
//                    val row =
//                        sheet.getRow(
//                            rowIndex
//                        )
//                            ?: continue
//
//                    val excelRowNumber =
//                        rowIndex + 1
//
//                    if (
//                        isRowEmpty(
//                            row = row,
//                            formatter = formatter
//                        )
//                    ) {
//                        println(
//                            "[EXCEL IMPORT] Skipped empty row=$excelRowNumber"
//                        )
//
//                        continue
//                    }
//
//                    println()
//                    println(
//                        "[EXCEL IMPORT] Processing row=$excelRowNumber"
//                    )
//
//                    try {
//                        val fullName =
//                            readCell(
//                                row = row,
//                                columnIndex =
//                                    headerIndexes.getValue(
//                                        FULL_NAME_HEADER
//                                    ),
//                                formatter = formatter
//                            )
//
//                        val className =
//                            readCell(
//                                row = row,
//                                columnIndex =
//                                    headerIndexes.getValue(
//                                        CURRENT_CLASS_HEADER
//                                    ),
//                                formatter = formatter
//                            )
//
//                        val contactOfFather =
//                            readCell(
//                                row = row,
//                                columnIndex =
//                                    headerIndexes.getValue(
//                                        FATHER_CONTACT_HEADER
//                                    ),
//                                formatter = formatter
//                            )
//
//                        val contactOfMother =
//                            headerIndexes[
//                                MOTHER_CONTACT_HEADER
//                            ]
//                                ?.let { columnIndex ->
//
//                                    readCell(
//                                        row = row,
//                                        columnIndex = columnIndex,
//                                        formatter = formatter
//                                    )
//                                }
//                                ?.takeIf { value ->
//                                    value.isNotBlank()
//                                }
//
//                        val discountedText =
//                            headerIndexes[
//                                DISCOUNTED_HEADER
//                            ]
//                                ?.let { columnIndex ->
//
//                                    readCell(
//                                        row = row,
//                                        columnIndex = columnIndex,
//                                        formatter = formatter
//                                    )
//                                }
//                                .orEmpty()
//
//                        println(
//                            "[EXCEL IMPORT] Parsed row=$excelRowNumber, " +
//                                    "fullName=$fullName, " +
//                                    "className=$className, " +
//                                    "contactOfFather=$contactOfFather, " +
//                                    "contactOfMother=$contactOfMother, " +
//                                    "discountedText=$discountedText"
//                        )
//
//                        require(
//                            fullName.isNotBlank()
//                        ) {
//                            "Student name is required."
//                        }
//
//                        require(
//                            className.isNotBlank()
//                        ) {
//                            "Current class is required for $fullName."
//                        }
//
//                        require(
//                            contactOfFather.isNotBlank()
//                        ) {
//                            "Father's contact is required for $fullName."
//                        }
//
//                        val normalizedClassName =
//                            normalizeClassName(
//                                className
//                            )
//
//                        val classId =
//                            classMap[
//                                normalizedClassName
//                            ]
//                                ?: throw IllegalArgumentException(
//                                    "Class '$className' was not found. " +
//                                            "Available classes: ${
//                                                classMap.keys.joinToString(", ")
//                                            }"
//                                )
//
//                        val isDiscounted =
//                            parseBoolean(
//                                discountedText
//                            )
//
//                        val studentRequest =
//                            CreateStudentRequest(
//                                user =
//                                    CreateUserPart(
//                                        fullName = fullName,
//                                        role = "student",
//
//                                    ),
//
//                                currentNewGradeClassId =
//                                    classId,
//
//                                family =
//                                    null,
//
//                                isDiscountedStudent =
//                                    isDiscounted,
//
//                                contactOfFather =
//                                    contactOfFather,
//
//                                contactOfMother =
//                                    contactOfMother,
//
//                                sendAdmissionSms =
//                                    sendAdmissionSms,
//
//                                admissionSmsMessage =
//                                    if (sendAdmissionSms) {
//
//                                        admissionSmsMessage
//                                            ?.trim()
//
//                                    } else {
//
//                                        null
//                                    }
//                            )
//
//                        println(
//                            "[EXCEL IMPORT] Student request=$studentRequest"
//                        )
//
//                        val createdStudent =
//                            StudentService.createStudent(
//                                tenantSchema =
//                                    tenantSchema,
//
//                                request =
//                                    studentRequest
//                            )
//
//                        importedCount += 1
//
//                        println(
//                            "[EXCEL IMPORT] SUCCESS row=$excelRowNumber, " +
//                                    "student=$fullName, " +
//                                    "classId=$classId, " +
//                                    "admissionSms=$sendAdmissionSms"
//                        )
//
//                        println(
//                            "[EXCEL IMPORT] Created student=$createdStudent"
//                        )
//
//                    } catch (exception: Exception) {
//                        failedCount += 1
//
//                        val errorMessage =
//                            exception.message
//                                ?: "Unable to import this student."
//
//                        errors.add(
//                            StudentImportError(
//                                rowNumber =
//                                    excelRowNumber,
//
//                                message =
//                                    errorMessage
//                            )
//                        )
//
//                        println(
//                            "[EXCEL IMPORT] FAILED row=$excelRowNumber"
//                        )
//
//                        println(
//                            "[EXCEL IMPORT] Error type=${exception::class.qualifiedName}"
//                        )
//
//                        println(
//                            "[EXCEL IMPORT] Error message=$errorMessage"
//                        )
//
//                        exception.printStackTrace()
//                    }
//                }
//            }
//        }
//
//        println()
//        println("======================================================")
//        println("[EXCEL IMPORT] IMPORT COMPLETED")
//        println("[EXCEL IMPORT] Source=FRONTEND UPLOAD")
//        println("[EXCEL IMPORT] originalFileName=$originalFileName")
//        println("[EXCEL IMPORT] fileBytes=${fileBytes.size}")
//        println("[EXCEL IMPORT] importedCount=$importedCount")
//        println("[EXCEL IMPORT] failedCount=$failedCount")
//        println("[EXCEL IMPORT] sendAdmissionSms=$sendAdmissionSms")
//        println("======================================================")
//
//        return StudentImportResult(
//            importedCount =
//                importedCount,
//
//            failedCount =
//                failedCount,
//
//            message =
//                if (failedCount == 0) {
//                    "All students were imported successfully."
//                } else {
//                    "Import completed with some failed rows."
//                },
//
//            errors =
//                errors
//        )
//    }
//
//    private fun loadClassMap(
//        tenantSchema: String
//    ): Map<String, Int> {
//
//        return transaction {
//
//            setTenantSchema(
//                tenantSchema
//            )
//
//            NewGradeClassTable
//                .selectAll()
//                .associate { row ->
//
//                    val className =
//                        normalizeClassName(
//                            row[
//                                NewGradeClassTable.name
//                            ]
//                        )
//
//                    val classId =
//                        row[
//                            NewGradeClassTable.id
//                        ].value
//
//                    className to classId
//                }
//        }
//    }
//
//    private fun buildHeaderIndexes(
//        headerRow: Row,
//        formatter: DataFormatter
//    ): Map<String, Int> {
//
//        val headerIndexes =
//            mutableMapOf<String, Int>()
//
//        for (cell in headerRow) {
//            val headerName =
//                normalizeHeader(
//                    formatter.formatCellValue(
//                        cell
//                    )
//                )
//
//            if (headerName.isNotBlank()) {
//                headerIndexes[
//                    headerName
//                ] = cell.columnIndex
//            }
//        }
//
//        return headerIndexes
//    }
//
//    private fun validateHeaders(
//        headerIndexes: Map<String, Int>
//    ) {
//
//        val requiredHeaders =
//            listOf(
//                FULL_NAME_HEADER,
//                CURRENT_CLASS_HEADER,
//                FATHER_CONTACT_HEADER
//            )
//
//        val missingHeaders =
//            requiredHeaders.filter { header ->
//
//                !headerIndexes.containsKey(
//                    header
//                )
//            }
//
//        require(
//            missingHeaders.isEmpty()
//        ) {
//            "Missing required Excel headers: ${
//                missingHeaders.joinToString(", ")
//            }. Required headers are fullName, currentClass and contactOfFather."
//        }
//    }
//
//    private fun readCell(
//        row: Row,
//        columnIndex: Int,
//        formatter: DataFormatter
//    ): String {
//
//        val cell =
//            row.getCell(
//                columnIndex,
//                Row.MissingCellPolicy.RETURN_BLANK_AS_NULL
//            )
//                ?: return ""
//
//        return formatter
//            .formatCellValue(
//                cell
//            )
//            .trim()
//    }
//
//    private fun isRowEmpty(
//        row: Row,
//        formatter: DataFormatter
//    ): Boolean {
//
//        if (
//            row.firstCellNum < 0 ||
//            row.lastCellNum < 0
//        ) {
//            return true
//        }
//
//        for (
//        columnIndex in
//        row.firstCellNum until row.lastCellNum
//        ) {
//            val cell =
//                row.getCell(
//                    columnIndex,
//                    Row.MissingCellPolicy.RETURN_BLANK_AS_NULL
//                )
//
//            if (
//                cell != null &&
//                formatter
//                    .formatCellValue(cell)
//                    .trim()
//                    .isNotBlank()
//            ) {
//                return false
//            }
//        }
//
//        return true
//    }
//
//    private fun normalizeHeader(
//        value: String
//    ): String {
//
//        return value
//            .trim()
//            .lowercase()
//            .replace(
//                Regex("[^a-z0-9]"),
//                ""
//            )
//    }
//
//    private fun normalizeClassName(
//        value: String
//    ): String {
//
//        return value
//            .trim()
//            .lowercase()
//            .replace(
//                Regex("\\s+"),
//                " "
//            )
//    }
//
//    private fun parseBoolean(
//        value: String
//    ): Boolean {
//
//        return when (
//            value
//                .trim()
//                .lowercase()
//        ) {
//            "yes",
//            "true",
//            "1",
//            "y" -> true
//
//            else -> false
//        }
//    }
//}
