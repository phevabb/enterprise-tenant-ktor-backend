package com.example.billing.routes

import com.example.billing.dto.CreateBillTemplateItemRequest
import com.example.billing.dto.CreateBillTemplateRequest
import com.example.billing.dto.GenerateIndividualBillsRequest
import com.example.billing.dto.RecordStudentBillPaymentRequest
import com.example.billing.dto.UpdateBillTemplateItemRequest
import com.example.billing.dto.UpdateBillTemplateRequest
import com.example.billing.dto.UpdateStudentBillStatusRequest
import com.example.billing.pdf.StudentBillPdfService
import com.example.billing.repos.BillTemplateRepository
import com.example.billing.repos.StudentBillRepository
import com.example.billing.repos.resolveTenantSchemaOrNull
import com.example.tenant.TenantSchemaService
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.billRoutes() {
    route("/api/billing") {

        /**
         * =====================================================
         * BILL TEMPLATES
         * =====================================================
         */

        post("/templates") {
            val tenantSchema = call.requireBillingTenantSchema() ?: return@post

            val request = call.receive<CreateBillTemplateRequest>()

            if (request.name.isBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Template name is required.")
                )
                return@post
            }

            if (request.categoryId <= 0) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Category is required.")
                )
                return@post
            }

            if (request.academicYearId <= 0) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Academic year is required.")
                )
                return@post
            }

            if (request.academicTermId <= 0) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Academic term is required.")
                )
                return@post
            }

            val template = BillTemplateRepository.create(
                tenantSchema = tenantSchema,
                request = request
            )

            call.respond(HttpStatusCode.Created, template)
        }

        get("/templates") {
            val tenantSchema = call.requireBillingTenantSchema() ?: return@get

            val templates = BillTemplateRepository.findAll(
                tenantSchema = tenantSchema
            )

            call.respond(HttpStatusCode.OK, templates)
        }

        get("/templates/{templateId}") {
            val tenantSchema = call.requireBillingTenantSchema() ?: return@get

            val templateId = call.parameters["templateId"]?.toIntOrNull()

            if (templateId == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Invalid template ID.")
                )
                return@get
            }

            val template = BillTemplateRepository.findById(
                tenantSchema = tenantSchema,
                templateId = templateId
            )

            if (template == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("message" to "Bill template not found.")
                )
                return@get
            }

            call.respond(HttpStatusCode.OK, template)
        }

        put("/templates/{templateId}") {
            val tenantSchema = call.requireBillingTenantSchema() ?: return@put

            val templateId = call.parameters["templateId"]?.toIntOrNull()

            if (templateId == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Invalid template ID.")
                )
                return@put
            }

            val request = call.receive<UpdateBillTemplateRequest>()

            val updated = BillTemplateRepository.updateTemplate(
                tenantSchema = tenantSchema,
                templateId = templateId,
                request = request
            )

            if (updated == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("message" to "Bill template not found.")
                )
                return@put
            }

            call.respond(HttpStatusCode.OK, updated)
        }

        patch("/templates/{templateId}/active") {
            val tenantSchema = call.requireBillingTenantSchema() ?: return@patch

            val templateId = call.parameters["templateId"]?.toIntOrNull()

            if (templateId == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Invalid template ID.")
                )
                return@patch
            }

            val request = call.receive<UpdateBillTemplateRequest>()

            val isActive = request.isActive

            if (isActive == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "isActive is required.")
                )
                return@patch
            }

            val updated = BillTemplateRepository.updateTemplateActiveStatus(
                tenantSchema = tenantSchema,
                templateId = templateId,
                isActive = isActive
            )

            if (updated == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("message" to "Bill template not found.")
                )
                return@patch
            }

            call.respond(HttpStatusCode.OK, updated)
        }

        delete("/templates/{templateId}") {
            val tenantSchema = call.requireBillingTenantSchema() ?: return@delete

            val templateId = call.parameters["templateId"]?.toIntOrNull()

            if (templateId == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Invalid template ID.")
                )
                return@delete
            }

            val deleted = BillTemplateRepository.deleteTemplate(
                tenantSchema = tenantSchema,
                templateId = templateId
            )

            if (!deleted) {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("message" to "Bill template not found.")
                )
                return@delete
            }

            call.respond(
                HttpStatusCode.OK,
                mapOf("message" to "Bill template deleted successfully.")
            )
        }

        /**
         * =====================================================
         * BILL TEMPLATE ITEMS
         * =====================================================
         */

        post("/templates/{templateId}/items") {
            val tenantSchema = call.requireBillingTenantSchema() ?: return@post

            val templateId = call.parameters["templateId"]?.toIntOrNull()

            if (templateId == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Invalid template ID.")
                )
                return@post
            }

            val request = call.receive<CreateBillTemplateItemRequest>()

            if (request.itemName.isBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Item name is required.")
                )
                return@post
            }

            val item = BillTemplateRepository.addItem(
                tenantSchema = tenantSchema,
                templateId = templateId,
                request = request
            )

            call.respond(HttpStatusCode.Created, item)
        }

        put("/templates/{templateId}/items/{itemId}") {
            val tenantSchema = call.requireBillingTenantSchema() ?: return@put

            val templateId = call.parameters["templateId"]?.toIntOrNull()
            val itemId = call.parameters["itemId"]?.toIntOrNull()

            if (templateId == null || itemId == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Invalid template ID or item ID.")
                )
                return@put
            }

            val request = call.receive<UpdateBillTemplateItemRequest>()

            val updated = BillTemplateRepository.updateItem(
                tenantSchema = tenantSchema,
                templateId = templateId,
                itemId = itemId,
                request = request
            )

            if (updated == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("message" to "Bill template item not found.")
                )
                return@put
            }

            call.respond(HttpStatusCode.OK, updated)
        }

        patch("/templates/{templateId}/items/{itemId}/active") {
            val tenantSchema = call.requireBillingTenantSchema() ?: return@patch

            val templateId = call.parameters["templateId"]?.toIntOrNull()
            val itemId = call.parameters["itemId"]?.toIntOrNull()

            if (templateId == null || itemId == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Invalid template ID or item ID.")
                )
                return@patch
            }

            val request = call.receive<UpdateBillTemplateItemRequest>()

            val isActive = request.isActive

            if (isActive == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "isActive is required.")
                )
                return@patch
            }

            val updated = BillTemplateRepository.updateItemActiveStatus(
                tenantSchema = tenantSchema,
                templateId = templateId,
                itemId = itemId,
                isActive = isActive
            )

            if (updated == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("message" to "Bill template item not found.")
                )
                return@patch
            }

            call.respond(HttpStatusCode.OK, updated)
        }

        delete("/templates/{templateId}/items/{itemId}") {
            val tenantSchema = call.requireBillingTenantSchema() ?: return@delete

            val templateId = call.parameters["templateId"]?.toIntOrNull()
            val itemId = call.parameters["itemId"]?.toIntOrNull()

            if (templateId == null || itemId == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Invalid template ID or item ID.")
                )
                return@delete
            }

            val deleted = BillTemplateRepository.deleteItem(
                tenantSchema = tenantSchema,
                templateId = templateId,
                itemId = itemId
            )

            if (!deleted) {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("message" to "Bill template item not found.")
                )
                return@delete
            }

            call.respond(
                HttpStatusCode.OK,
                mapOf("message" to "Bill template item deleted successfully.")
            )
        }

        /**
         * =====================================================
         * GENERATE INDIVIDUAL STUDENT BILLS
         * =====================================================
         */

        post("/templates/{templateId}/generate-individual-bills") {
            val tenantSchema = call.requireBillingTenantSchema() ?: return@post

            val templateId = call.parameters["templateId"]?.toIntOrNull()

            if (templateId == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Invalid template ID.")
                )
                return@post
            }

            val request = call.receive<GenerateIndividualBillsRequest>()

            val result = StudentBillRepository.generateIndividualBillsFromTemplate(
                tenantSchema = tenantSchema,
                billTemplateId = templateId,
                dueDateEpochMillis = request.dueDateEpochMillis
            )

            call.respond(HttpStatusCode.OK, result)
        }

        /**
         * =====================================================
         * STUDENT BILLS
         * =====================================================
         */

        get("/student-bills") {
            val tenantSchema = call.requireBillingTenantSchema() ?: return@get

            val status = call.request.queryParameters["status"]

            val bills = if (status.isNullOrBlank()) {
                StudentBillRepository.findAllStudentBills(
                    tenantSchema = tenantSchema
                )
            } else {
                StudentBillRepository.findBillsByStatus(
                    tenantSchema = tenantSchema,
                    status = status
                )
            }

            call.respond(HttpStatusCode.OK, bills)
        }

        get("/student-bills/{billId}") {
            val tenantSchema = call.requireBillingTenantSchema() ?: return@get

            val billId = call.parameters["billId"]?.toIntOrNull()

            if (billId == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Invalid bill ID.")
                )
                return@get
            }

            val bill = StudentBillRepository.findBillById(
                tenantSchema = tenantSchema,
                billId = billId
            )

            if (bill == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("message" to "Student bill not found.")
                )
                return@get
            }

            call.respond(HttpStatusCode.OK, bill)
        }

        get("/student-bills/{billId}/pdf") {
            val tenantSchema = call.requireBillingTenantSchema() ?: return@get

            val billId = call.parameters["billId"]?.toIntOrNull()

            if (billId == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Invalid bill ID.")
                )
                return@get
            }

            val bill = StudentBillRepository.findBillById(
                tenantSchema = tenantSchema,
                billId = billId
            )

            if (bill == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("message" to "Student bill not found.")
                )
                return@get
            }

            val schoolName = call.resolveSchoolNameOrDefault(
                defaultName = "Phena School"
            )

            val pdfBytes = StudentBillPdfService.generateStudentBillPdf(
                bill = bill,
                schoolName = schoolName,
                supportEmail = "support@phenaschool.com"
            )

            val fileName = "${bill.billNumber}.pdf"

            call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Attachment
                    .withParameter(ContentDisposition.Parameters.FileName, fileName)
                    .toString()
            )

            call.respondBytes(
                bytes = pdfBytes,
                contentType = ContentType.Application.Pdf,
                status = HttpStatusCode.OK
            )
        }

        get("/students/{studentId}/bills") {
            val tenantSchema = call.requireBillingTenantSchema() ?: return@get

            val studentId = call.parameters["studentId"]?.toIntOrNull()

            if (studentId == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Invalid student ID.")
                )
                return@get
            }

            val bills = StudentBillRepository.findBillsByStudent(
                tenantSchema = tenantSchema,
                studentId = studentId
            )

            call.respond(HttpStatusCode.OK, bills)
        }

        get("/templates/{templateId}/student-bills") {
            val tenantSchema = call.requireBillingTenantSchema() ?: return@get

            val templateId = call.parameters["templateId"]?.toIntOrNull()

            if (templateId == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Invalid template ID.")
                )
                return@get
            }

            val bills = StudentBillRepository.findBillsByTemplate(
                tenantSchema = tenantSchema,
                templateId = templateId
            )

            call.respond(HttpStatusCode.OK, bills)
        }

        patch("/student-bills/{billId}/status") {
            val tenantSchema = call.requireBillingTenantSchema() ?: return@patch

            val billId = call.parameters["billId"]?.toIntOrNull()

            if (billId == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Invalid bill ID.")
                )
                return@patch
            }

            val request = call.receive<UpdateStudentBillStatusRequest>()

            if (request.status.isBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Status is required.")
                )
                return@patch
            }

            val updated = StudentBillRepository.updateBillStatus(
                tenantSchema = tenantSchema,
                billId = billId,
                status = request.status
            )

            if (updated == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("message" to "Student bill not found.")
                )
                return@patch
            }

            call.respond(HttpStatusCode.OK, updated)
        }

        post("/student-bills/{billId}/payments") {
            val tenantSchema = call.requireBillingTenantSchema() ?: return@post

            val billId = call.parameters["billId"]?.toIntOrNull()

            if (billId == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Invalid bill ID.")
                )
                return@post
            }

            val request = call.receive<RecordStudentBillPaymentRequest>()

            if (request.amountPaidCedis <= 0) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Payment amount must be greater than zero.")
                )
                return@post
            }

            val updated = StudentBillRepository.recordPayment(
                tenantSchema = tenantSchema,
                billId = billId,
                amountPaidCedis = request.amountPaidCedis
            )

            if (updated == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("message" to "Student bill not found.")
                )
                return@post
            }

            call.respond(HttpStatusCode.OK, updated)
        }
    }
}

private suspend fun ApplicationCall.requireBillingTenantSchema(): String? {
    val tenantSchema = resolveTenantSchemaOrNull()

    if (tenantSchema.isNullOrBlank()) {
        respond(
            HttpStatusCode.BadRequest,
            mapOf("message" to "Tenant schema could not be resolved.")
        )
        return null
    }

    TenantSchemaService.ensureBillingTablesForTenant(tenantSchema)

    return tenantSchema
}