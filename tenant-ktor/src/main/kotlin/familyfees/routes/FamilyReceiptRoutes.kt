package com.example.familyfees.routes

import com.example.familyfees.repos.FamilyReceiptRepository
import com.example.familyfees.utils.FamilyReceiptPdfGenerator
import com.example.tenant.currentTenant
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.familyReceiptRoutes() {

    // ✅ GET /api/family-receipt/{id}
    get("{id}") {
        val tenant = call.currentTenant()
        val tenantSchema = tenant.tenantSchema

        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Invalid id")
            )

        val receipt = FamilyReceiptRepository.findById(tenantSchema, id)
            ?: return@get call.respond(
                HttpStatusCode.NotFound,
                mapOf("error" to "Receipt not found")
            )

        call.respond(HttpStatusCode.OK, receipt)
    }

    // ✅ GET /api/family-receipt/{id}/pdf?schoolName=Phevab%20Academy
    get("{id}/pdf") {
        val tenant = call.currentTenant()
        val tenantSchema = tenant.tenantSchema

        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Invalid id")
            )

        val schoolName = call.request.queryParameters["schoolName"]
            ?.trim()
            ?.take(120)
            ?.takeIf { it.isNotBlank() }
            ?: "School"

        println("DEBUG FamilyReceipt PDF schoolName = $schoolName")

        val r = FamilyReceiptRepository.findById(tenantSchema, id)
            ?: return@get call.respond(
                HttpStatusCode.NotFound,
                mapOf("error" to "Receipt not found")
            )

        val pdfBytes = FamilyReceiptPdfGenerator.buildPdf(
            r = r,
            schoolName = schoolName
        )

        call.response.header(
            HttpHeaders.ContentDisposition,
            ContentDisposition.Attachment
                .withParameter(
                    ContentDisposition.Parameters.FileName,
                    "${r.receiptNo}.pdf"
                )
                .toString()
        )

        call.respondBytes(
            bytes = pdfBytes,
            contentType = ContentType.Application.Pdf,
            status = HttpStatusCode.OK
        )
    }
}