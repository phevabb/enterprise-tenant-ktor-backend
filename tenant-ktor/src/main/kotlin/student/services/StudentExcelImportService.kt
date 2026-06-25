package com.example.student.services




import com.example.academics.repos.setTenantSchema
import com.example.student.dtos.requests.CreateStudentRequest
import com.example.student.dtos.requests.CreateUserPart
import com.example.student.dtos.response.StudentImportError
import com.example.student.dtos.response.StudentImportResponse
import com.example.student.tables.NewGradeClassTable
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.InputStream

object StudentExcelImportService {

    private val requiredHeaders = listOf(
        "fullName",
        "currentClass",
        "contactOfFather",
        "contactOfMother"
    )

    fun importFromExcel(
        tenantSchema: String,
        inputStream: InputStream
    ): StudentImportResponse {
        val classMap = loadClassMap(tenantSchema)

        val errors = mutableListOf<StudentImportError>()
        var importedCount = 0

        XSSFWorkbook(inputStream).use { workbook ->
            val sheet = workbook.getSheetAt(0)
                ?: return StudentImportResponse(
                    message = "No sheet found in Excel file.",
                    importedCount = 0,
                    failedCount = 1,
                    errors = listOf(
                        StudentImportError(
                            rowNumber = 0,
                            message = "No sheet found in Excel file."
                        )
                    )
                )

            val formatter = DataFormatter()

            val headerRow = sheet.getRow(0)
                ?: return StudentImportResponse(
                    message = "Header row is missing.",
                    importedCount = 0,
                    failedCount = 1,
                    errors = listOf(
                        StudentImportError(
                            rowNumber = 1,
                            message = "Header row is missing."
                        )
                    )
                )

            val headers = requiredHeaders.indices.map { index ->
                formatter.formatCellValue(headerRow.getCell(index)).trim()
            }

            if (headers != requiredHeaders) {
                return StudentImportResponse(
                    message = "Invalid Excel template.",
                    importedCount = 0,
                    failedCount = 1,
                    errors = listOf(
                        StudentImportError(
                            rowNumber = 1,
                            message = "Headers must be exactly: ${requiredHeaders.joinToString(", ")}"
                        )
                    )
                )
            }

            val lastRowNumber = sheet.lastRowNum

            for (rowIndex in 1..lastRowNumber) {
                val row = sheet.getRow(rowIndex) ?: continue
                val rowNumber = rowIndex + 1

                try {
                    val fullName = formatter.formatCellValue(row.getCell(0)).trim()
                    val currentClass = formatter.formatCellValue(row.getCell(1)).trim()
                    val contactOfFather = formatter.formatCellValue(row.getCell(2)).trim()
                    val contactOfMother = formatter.formatCellValue(row.getCell(3)).trim()

                    if (fullName.isBlank()) {
                        errors += StudentImportError(
                            rowNumber = rowNumber,
                            message = "fullName is required."
                        )
                        continue
                    }

                    if (currentClass.isBlank()) {
                        errors += StudentImportError(
                            rowNumber = rowNumber,
                            message = "currentClass is required."
                        )
                        continue
                    }

                    val classId = classMap[normalizeClassName(currentClass)]

                    if (classId == null) {
                        errors += StudentImportError(
                            rowNumber = rowNumber,
                            message = "Class '$currentClass' was not found. Please create this class first and make sure the spelling matches."
                        )
                        continue
                    }

                    if (contactOfFather.isBlank()) {
                        errors += StudentImportError(
                            rowNumber = rowNumber,
                            message = "contactOfFather is required."
                        )
                        continue
                    }

                    if (contactOfMother.isBlank()) {
                        errors += StudentImportError(
                            rowNumber = rowNumber,
                            message = "contactOfMother is required."
                        )
                        continue
                    }

                    StudentService.createStudent(
                        tenantSchema = tenantSchema,
                        request = CreateStudentRequest(
                            user = CreateUserPart(
                                fullName = fullName,
                                role = "student",
                                isActive = true,
                                isStaff = false
                            ),
                            currentNewGradeClassId = classId,
                            family = null,
                            contactOfFather = contactOfFather,
                            contactOfMother = contactOfMother,
                            isDiscountedStudent = false
                        )
                    )

                    importedCount++
                } catch (e: Exception) {
                    errors += StudentImportError(
                        rowNumber = rowNumber,
                        message = e.message ?: "Unable to import row."
                    )
                }
            }
        }

        return StudentImportResponse(
            message = "Student import completed.",
            importedCount = importedCount,
            failedCount = errors.size,
            errors = errors
        )
    }

    private fun loadClassMap(
        tenantSchema: String
    ): Map<String, Int> = transaction {
        setTenantSchema(tenantSchema)

        NewGradeClassTable
            .selectAll()
            .associate { row ->
                normalizeClassName(row[NewGradeClassTable.name]) to row[NewGradeClassTable.id].value
            }
    }

    private fun normalizeClassName(value: String): String {
        return value
            .trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")
    }
}