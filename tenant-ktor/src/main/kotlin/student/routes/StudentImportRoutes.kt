package com.example.student.routes




import com.example.student.services.StudentExcelImportService
import com.example.tenant.currentTenant
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.ByteArrayOutputStream

fun Route.studentImportRoutes() {
    route("/") {

        get("template") {
            val bytes = buildStudentImportTemplate()

            call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Attachment
                    .withParameter(
                        ContentDisposition.Parameters.FileName,
                        "students_import_template.xlsx"
                    )
                    .toString()
            )

            call.respondBytes(
                bytes = bytes,
                contentType = ContentType.parse(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                )
            )
        }

        post("excel") {
            val tenant = call.currentTenant()
            val tenantSchema = tenant.tenantSchema

            var imported = false
            var response: Any? = null

            val multipart = call.receiveMultipart()

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        if (part.name == "file") {
                            part.streamProvider().use { inputStream ->
                                response = StudentExcelImportService.importFromExcel(
                                    tenantSchema = tenantSchema,
                                    inputStream = inputStream
                                )
                            }

                            imported = true
                        }
                    }

                    else -> Unit
                }

                part.dispose()
            }

            if (!imported) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Excel file is required.")
                )
                return@post
            }

            call.respond(HttpStatusCode.OK, response!!)
        }
    }
}
private fun buildStudentImportTemplate(): ByteArray {
    XSSFWorkbook().use { workbook ->
        val sheet = workbook.createSheet("students")

        val headerRow = sheet.createRow(0)

        val headers = listOf(
            "fullName",
            "currentClass",
            "contactOfFather",
            "contactOfMother"
        )

        headers.forEachIndexed { index, header ->
            headerRow.createCell(index).setCellValue(header)
            sheet.setColumnWidth(index, 7000)
        }

        val sampleRow = sheet.createRow(1)
        sampleRow.createCell(0).setCellValue("Kwame Mensah")
        sampleRow.createCell(1).setCellValue("Class 1")
        sampleRow.createCell(2).setCellValue("0240000000")
        sampleRow.createCell(3).setCellValue("0550000000")

        ByteArrayOutputStream().use { output ->
            workbook.write(output)
            return output.toByteArray()
        }
    }
}
