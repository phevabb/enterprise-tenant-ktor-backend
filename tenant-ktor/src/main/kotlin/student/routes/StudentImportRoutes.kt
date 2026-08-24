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
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import student.imports.ImportStudentsFromExcel
import java.io.ByteArrayOutputStream
import kotlin.text.get

fun Route.studentImportRoutes() {
    route("/") {


        post("excel") {

            try {
                val tenant =
                    call.currentTenant()

                val tenantSchema =
                    tenant.tenantSchema

                var uploadedFileBytes:
                        ByteArray? = null

                var originalFileName:
                        String? = null

                var uploadedContentType:
                        String? = null

                var sendAdmissionSms =
                    false

                var admissionSmsMessage:
                        String? = null

                println()
                println("======================================================")
                println("[STUDENT IMPORT ROUTE] Route hit")
                println("[STUDENT IMPORT ROUTE] tenantSchema=$tenantSchema")
                println(
                    "[STUDENT IMPORT ROUTE] requestContentType=${
                        call.request.headers["Content-Type"]
                    }"
                )
                println("======================================================")

                val multipart =
                    call.receiveMultipart()

                multipart.forEachPart { part ->

                    when (part) {
                        is PartData.FileItem -> {
                            println(
                                "[STUDENT IMPORT ROUTE] File part received"
                            )

                            println(
                                "[STUDENT IMPORT ROUTE] partName=${part.name}"
                            )

                            println(
                                "[STUDENT IMPORT ROUTE] originalFileName=${part.originalFileName}"
                            )

                            println(
                                "[STUDENT IMPORT ROUTE] contentType=${part.contentType}"
                            )

                            if (
                                part.name == "file" ||
                                uploadedFileBytes == null
                            ) {
                                originalFileName =
                                    part.originalFileName
                                        ?: "students.xlsx"

                                uploadedContentType =
                                    part.contentType
                                        ?.toString()

                                uploadedFileBytes =
                                    part.provider()
                                        .readRemaining()
                                        .readByteArray()

                                println(
                                    "[STUDENT IMPORT ROUTE] Uploaded bytes read=${uploadedFileBytes?.size}"
                                )
                            }
                        }

                        is PartData.FormItem -> {
                            println(
                                "[STUDENT IMPORT ROUTE] Form item: ${part.name}=${part.value}"
                            )

                            when (part.name) {
                                "sendAdmissionSms" -> {
                                    sendAdmissionSms =
                                        part.value.equals(
                                            other = "true",
                                            ignoreCase = true
                                        )
                                }

                                "admissionSmsMessage" -> {
                                    admissionSmsMessage =
                                        part.value
                                            .trim()
                                            .takeIf { value ->
                                                value.isNotBlank()
                                            }
                                }
                            }
                        }

                        else -> {
                            println(
                                "[STUDENT IMPORT ROUTE] Ignored multipart part=${part::class.simpleName}"
                            )
                        }
                    }

                    part.dispose()
                }

                val safeFileBytes =
                    uploadedFileBytes
                        ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf(
                                "message" to
                                        "No Excel file was uploaded."
                            )
                        )

                val safeOriginalFileName =
                    originalFileName
                        ?: "students.xlsx"

                println()
                println("======================================================")
                println("[STUDENT IMPORT ROUTE] Multipart processing completed")
                println("[STUDENT IMPORT ROUTE] originalFileName=$safeOriginalFileName")
                println("[STUDENT IMPORT ROUTE] uploadedContentType=$uploadedContentType")
                println("[STUDENT IMPORT ROUTE] uploadedFileSize=${safeFileBytes.size}")
                println("[STUDENT IMPORT ROUTE] sendAdmissionSms=$sendAdmissionSms")
                println(
                    "[STUDENT IMPORT ROUTE] admissionSmsMessagePresent=${
                        !admissionSmsMessage.isNullOrBlank()
                    }"
                )
                println("[STUDENT IMPORT ROUTE] tenantSchema=$tenantSchema")
                println("======================================================")

                require(
                    safeOriginalFileName
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

                val result =
                    ImportStudentsFromExcel.run(
                        tenantSchema =
                            tenantSchema,

                        fileBytes =
                            safeFileBytes,

                        originalFileName =
                            safeOriginalFileName,

                        sendAdmissionSms =
                            sendAdmissionSms,

                        admissionSmsMessage =
                            admissionSmsMessage
                    )

                println(
                    "[STUDENT IMPORT ROUTE] Import result=$result"
                )

                call.respond(
                    HttpStatusCode.OK,
                    result
                )

            } catch (exception: IllegalArgumentException) {
                println(
                    "[STUDENT IMPORT ROUTE] Validation failed: ${exception.message}"
                )

                exception.printStackTrace()

                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "message" to (
                                exception.message
                                    ?: "Invalid student import request."
                                )
                    )
                )

            } catch (exception: Exception) {
                println(
                    "[STUDENT IMPORT ROUTE] Import failed: ${exception.message}"
                )

                println(
                    "[STUDENT IMPORT ROUTE] Error type=${exception::class.qualifiedName}"
                )

                exception.printStackTrace()

                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "message" to (
                                exception.message
                                    ?: "Unable to import students."
                                )
                    )
                )
            }
        }


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

//        post("excel") {
//            val tenant = call.currentTenant()
//            val tenantSchema = tenant.tenantSchema
//
//            var imported = false
//            var response: Any? = null
//
//            val multipart = call.receiveMultipart()
//
//            multipart.forEachPart { part ->
//                when (part) {
//                    is PartData.FileItem -> {
//                        if (part.name == "file") {
//                            part.streamProvider().use { inputStream ->
//                                response = StudentExcelImportService.importFromExcel(
//                                    tenantSchema = tenantSchema,
//                                    inputStream = inputStream
//                                )
//                            }
//
//                            imported = true
//                        }
//                    }
//
//                    else -> Unit
//                }
//
//                part.dispose()
//            }
//
//            if (!imported) {
//                call.respond(
//                    HttpStatusCode.BadRequest,
//                    mapOf("message" to "Excel file is required.")
//                )
//                return@post
//            }
//
//            call.respond(HttpStatusCode.OK, response!!)
//        }
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






















