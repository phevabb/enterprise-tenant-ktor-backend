package com.example.fees.routes



import com.example.fees.repos.ReceiptRepository
import com.example.fees.utils.ReceiptPdfGenerator
import com.example.tenant.currentTenant
import io.ktor.http.*
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.*

fun Route.receiptRoutes() {

    // Helper: get tenant safely
    fun ApplicationCall.tenantSchema(): String {
        return this.currentTenant().tenantSchema
    }

    // GET /api/receipts/{id}
    get("{id}") {

        val tenantSchema = call.tenantSchema()

        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Invalid id")
            )

        val receipt = ReceiptRepository.findById(tenantSchema, id)
            ?: return@get call.respond(
                HttpStatusCode.NotFound,
                mapOf("error" to "Receipt not found")
            )

        call.respond(HttpStatusCode.OK, receipt)
    }

    // GET /api/receipts/{id}/print
    get("{id}/print") {

        val tenantSchema = call.tenantSchema()

        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@get call.respondText(
                "Invalid id",
                status = HttpStatusCode.BadRequest
            )

        val r = ReceiptRepository.findById(tenantSchema, id)
            ?: return@get call.respondText(
                "Receipt not found",
                status = HttpStatusCode.NotFound
            )

        val tenant = call.currentTenant()

        val html = """
            <!doctype html>
            <html>
            <head>
              <meta charset="utf-8"/>
              <title>Receipt ${r.receiptNo}</title>
              <style>
                body { font-family: Arial, sans-serif; margin: 28px; }
                .box { border: 1px solid #e2e8f0; border-radius: 12px; padding: 18px; }
                .hdr { display:flex; justify-content:space-between; gap:12px; }
                .brand { font-weight:800; font-size:18px; color:#0f172a; }
                .muted { color:#64748b; font-size:12px; }
                table { width:100%; margin-top:14px; border-collapse: collapse; }
                td { padding:8px; border-bottom:1px solid #eef2f7; }
                .total { font-size:16px; font-weight:800; }
                .right { text-align:right; }
                @media print { button { display:none } }
              </style>
            </head>
            <body>
              <div class="box">
                <div class="hdr">
                  <div>
                    <div class="brand">${tenant.schoolName}</div>


                    <div class="muted">Official Payment Receipt</div>
                  </div>
                  <div class="right">
                    <div class="muted">Receipt No</div>
                    <div><b>${r.receiptNo}</b></div>
                    <div class="muted" style="margin-top:6px;">Date</div>
                    <div><b>${java.time.Instant.ofEpochMilli(r.createdAt)}</b></div>
                  </div>
                </div>

                <table>
                  <tr><td class="muted">Student</td><td class="right"><b>${r.studentName}</b></td></tr>
                  <tr><td class="muted">Class</td><td class="right"><b>${r.className}</b></td></tr>
                  <tr><td class="muted">Term / Year</td><td class="right"><b>${r.termName} / ${r.academicYearName}</b></td></tr>
                  <tr><td class="muted">Payment Method</td><td class="right"><b>${r.paymentMethod ?: "—"}</b></td></tr>
                  <tr><td class="muted">Amount Paid</td><td class="right total">GH₵ ${r.amountPaid}</td></tr>
                  <tr><td class="muted">Balance After</td><td class="right"><b>GH₵ ${r.balanceAfter}</b></td></tr>
                </table>

                <div class="muted" style="margin-top:14px;">
                  This receipt was generated electronically. No signature required.
                </div>

                <div>
                  <button onclick="window.print()">Print Receipt</button>
                </div>
              </div>
            </body>
            </html>
        """.trimIndent()

        call.respondText(html, ContentType.Text.Html)
    }

    // GET /api/receipts/{id}/pdf
    get("{id}/pdf") {

        val tenantSchema = call.tenantSchema()

        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Invalid id")
            )

        val r = ReceiptRepository.findById(tenantSchema, id)
            ?: return@get call.respond(
                HttpStatusCode.NotFound,
                mapOf("error" to "Receipt not found")
            )
        val tenant = call.currentTenant()


        val pdfBytes = ReceiptPdfGenerator.buildPdf(r, tenant.schoolName)

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