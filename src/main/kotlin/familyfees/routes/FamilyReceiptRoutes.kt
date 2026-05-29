package com.example.familyfees.routes




import com.example.familyfees.repos.FamilyReceiptRepository
import com.example.familyfees.utils.FamilyReceiptPdfGenerator
import io.ktor.http.*
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.*
import kotlin.text.trimIndent





import io.ktor.http.*
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.*

fun Route.familyReceiptRoutes() {

    // ✅ GET /api/family-receipts/{id}
    get("{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid id"))

        val receipt = FamilyReceiptRepository.findById(id)
            ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Receipt not found"))

        call.respond(HttpStatusCode.OK, receipt)
    }

    // ✅ GET /api/family-receipts/{id}/pdf
    get("{id}/pdf") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid id"))

        val r = FamilyReceiptRepository.findById(id)
            ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Receipt not found"))

        val pdfBytes = FamilyReceiptPdfGenerator.buildPdf(r)

        call.response.header(
            HttpHeaders.ContentDisposition,
            ContentDisposition.Attachment
                .withParameter(ContentDisposition.Parameters.FileName, "${r.receiptNo}.pdf")
                .toString()
        )

        call.respondBytes(
            bytes = pdfBytes,
            contentType = ContentType.Application.Pdf,
            status = HttpStatusCode.OK
        )
    }
}