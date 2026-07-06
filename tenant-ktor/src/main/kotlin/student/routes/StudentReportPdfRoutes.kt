package com.example.student.routes

import com.example.academics.repos.StudentAcademicRecordRepository
import com.example.academics.repos.setTenantSchema
import com.example.billing.dto.StudentReportCardListItemResponse

import com.example.student.services.StudentReportPdfService
import com.example.tenant.tables.TenantsTable
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.ByteArrayContent
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.studentReportPdfRoutes() {
    val pdfService = StudentReportPdfService()

    route("/cards") {

        /**
         * List report cards by term.
         *
         * GET /api/report/cards/student/{userId}
         */
        get("/student/{userId}") {
            val userId = call.parameters["userId"]

            if (userId.isNullOrBlank()) {
                call.respond(
                    status = HttpStatusCode.BadRequest,
                    message = mapOf("message" to "Invalid userId")
                )
                return@get
            }

            val tenantSchema = call.resolveTenantSchemaByCodeOrSlug()

            if (tenantSchema.isNullOrBlank()) {
                call.respond(
                    status = HttpStatusCode.BadRequest,
                    message = mapOf("message" to "Tenant schema could not be resolved.")
                )
                return@get
            }

            val reportCards = StudentAcademicRecordRepository.findAllByUserId(
                tenantSchema = tenantSchema,
                userId = userId
            )

            val response = reportCards.map { record ->
                val total = record.rawScoreTotal
                    ?: record.subjects.sumOf { subject -> subject.totalScore ?: 0 }

                val average = if (record.subjects.isEmpty()) {
                    0.0
                } else {
                    record.subjects.mapNotNull { it.totalScore }.average()
                }

                StudentReportCardListItemResponse(
                    id = record.id,
                    studentName = record.student.name,
                    studentUserId = userId,
                    profilePictureUrl = record.student.profilePictureUrl,
                    academicYearName = record.academicYear.name,
                    termName = record.term.name,
                    className = record.classLevel.name,
                    rawScoreTotal = total,
                    averageScore = average,
                    overallPosition = record.overallPosition,
                    numberOnRoll = record.numberOnRoll,
                    subjectCount = record.subjects.size
                )
            }

            call.respond(HttpStatusCode.OK, response)
        }

        /**
         * Download all terms as one PDF pack.
         *
         * GET /api/report/cards/student/{userId}/pdf
         */
        get("/student/{userId}/pdf") {
            val userId = call.parameters["userId"]

            if (userId.isNullOrBlank()) {
                call.respond(
                    status = HttpStatusCode.BadRequest,
                    message = "Invalid userId"
                )
                return@get
            }

            val tenantSchema = call.resolveTenantSchemaByCodeOrSlug()

            if (tenantSchema.isNullOrBlank()) {
                call.respond(
                    status = HttpStatusCode.BadRequest,
                    message = "Tenant schema could not be resolved from tenant code or tenant slug."
                )
                return@get
            }

            val reportCards = StudentAcademicRecordRepository.findAllByUserId(
                tenantSchema = tenantSchema,
                userId = userId
            )

            if (reportCards.isEmpty()) {
                call.respond(
                    status = HttpStatusCode.NotFound,
                    message = "No report cards found for this student"
                )
                return@get
            }

            val schoolName = call.resolveSchoolNameOrDefault(
                defaultName = "School Name"
            )

            val pdfBytes = pdfService.generateReportPack(
                schoolName = schoolName,
                records = reportCards
            )

            call.response.headers.append(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Attachment
                    .withParameter(
                        ContentDisposition.Parameters.FileName,
                        "student-report-pack-$userId.pdf"
                    )
                    .toString()
            )

            call.respond(
                ByteArrayContent(
                    bytes = pdfBytes,
                    contentType = ContentType.Application.Pdf
                )
            )
        }

        /**
         * Download one term/report card only.
         *
         * GET /api/report/cards/student/{userId}/terms/{reportCardId}/pdf
         */
        get("/student/{userId}/terms/{reportCardId}/pdf") {
            val userId = call.parameters["userId"]
            val reportCardId = call.parameters["reportCardId"]?.toIntOrNull()

            if (userId.isNullOrBlank() || reportCardId == null) {
                call.respond(
                    status = HttpStatusCode.BadRequest,
                    message = "Invalid userId or reportCardId"
                )
                return@get
            }

            val tenantSchema = call.resolveTenantSchemaByCodeOrSlug()

            if (tenantSchema.isNullOrBlank()) {
                call.respond(
                    status = HttpStatusCode.BadRequest,
                    message = "Tenant schema could not be resolved from tenant code or tenant slug."
                )
                return@get
            }

            val reportCards = StudentAcademicRecordRepository.findAllByUserId(
                tenantSchema = tenantSchema,
                userId = userId
            )

            val selectedReport = reportCards.firstOrNull { record ->
                record.id == reportCardId
            }

            if (selectedReport == null) {
                call.respond(
                    status = HttpStatusCode.NotFound,
                    message = "Report card not found for this term"
                )
                return@get
            }

            val schoolName = call.resolveSchoolNameOrDefault(
                defaultName = "School Name"
            )

            val pdfBytes = pdfService.generateReportPack(
                schoolName = schoolName,
                records = listOf(selectedReport)
            )

            val safeTermName = selectedReport.term.name
                .replace(" ", "-")
                .lowercase()

            val safeYearName = selectedReport.academicYear.name
                .replace("/", "-")
                .replace(" ", "-")
                .lowercase()

            call.response.headers.append(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Attachment
                    .withParameter(
                        ContentDisposition.Parameters.FileName,
                        "report-card-$userId-$safeYearName-$safeTermName.pdf"
                    )
                    .toString()
            )

            call.respond(
                ByteArrayContent(
                    bytes = pdfBytes,
                    contentType = ContentType.Application.Pdf
                )
            )
        }
    }
}

/**
 * Resolve tenant schema from:
 * 1. X-Tenant-Schema
 * 2. X-Tenant-Code
 * 3. X-Tenant-Slug
 */
private fun ApplicationCall.resolveTenantSchemaByCodeOrSlug(): String? {
    val tenantSchemaHeader = request.header("X-Tenant-Schema")

    if (!tenantSchemaHeader.isNullOrBlank()) {
        return tenantSchemaHeader
    }

    val tenantCode = request.header("X-Tenant-Code")
    val tenantSlug = request.header("X-Tenant-Slug")

    if (tenantCode.isNullOrBlank() && tenantSlug.isNullOrBlank()) {
        return null
    }

    return transaction {
        setTenantSchema("public")

        TenantsTable
            .selectAll()
            .firstOrNull { row ->
                val codeMatches =
                    !tenantCode.isNullOrBlank() &&
                            row[TenantsTable.tenantCode].equals(
                                tenantCode,
                                ignoreCase = true
                            )

                val slugMatches =
                    !tenantSlug.isNullOrBlank() &&
                            row[TenantsTable.tenantSlug].equals(
                                tenantSlug,
                                ignoreCase = true
                            )

                codeMatches || slugMatches
            }
            ?.get(TenantsTable.tenantSchema)
    }
}

private fun ApplicationCall.resolveSchoolNameOrDefault(
    defaultName: String = "School Name"
): String {
    val schoolNameHeader = request.header("X-School-Name")

    if (!schoolNameHeader.isNullOrBlank()) {
        return schoolNameHeader
    }

    val tenantSchemaHeader = request.header("X-Tenant-Schema")
    val tenantCode = request.header("X-Tenant-Code")
    val tenantSlug = request.header("X-Tenant-Slug")

    return transaction {
        setTenantSchema("public")

        val tenantRow = TenantsTable
            .selectAll()
            .firstOrNull { row ->
                val schemaMatches =
                    !tenantSchemaHeader.isNullOrBlank() &&
                            row[TenantsTable.tenantSchema].equals(
                                tenantSchemaHeader,
                                ignoreCase = true
                            )

                val codeMatches =
                    !tenantCode.isNullOrBlank() &&
                            row[TenantsTable.tenantCode].equals(
                                tenantCode,
                                ignoreCase = true
                            )

                val slugMatches =
                    !tenantSlug.isNullOrBlank() &&
                            row[TenantsTable.tenantSlug].equals(
                                tenantSlug,
                                ignoreCase = true
                            )

                schemaMatches || codeMatches || slugMatches
            }

        /**
         * If your TenantsTable uses another column, change this line.
         */
        tenantRow?.get(TenantsTable.schoolName) ?: defaultName
    }
}