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


private data class SchoolBrandingInfo(
    val schoolName: String,
    val schoolLogoUrl: String?,
    val schoolMotto: String?
)

private fun normalizeTenantCode(
    tenantCode: String
): String {
    return tenantCode
        .trim()
        .lowercase()
        .replace(
            Regex("[^a-z0-9_]"),
            ""
        )
}


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
//        get("/student/{userId}/pdf") {
//            val userId = call.parameters["userId"]
//
//            if (userId.isNullOrBlank()) {
//                call.respond(
//                    status = HttpStatusCode.BadRequest,
//                    message = "Invalid userId"
//                )
//                return@get
//            }
//
//            val tenantSchema = call.resolveTenantSchemaByCodeOrSlug()
//
//            if (tenantSchema.isNullOrBlank()) {
//                call.respond(
//                    status = HttpStatusCode.BadRequest,
//                    message = "Tenant schema could not be resolved from tenant code or tenant slug."
//                )
//                return@get
//            }
//
//            val reportCards = StudentAcademicRecordRepository.findAllByUserId(
//                tenantSchema = tenantSchema,
//                userId = userId
//            )
//
//            if (reportCards.isEmpty()) {
//                call.respond(
//                    status = HttpStatusCode.NotFound,
//                    message = "No report cards found for this student"
//                )
//                return@get
//            }
//
//            val schoolBranding =
//                call.resolveSchoolBrandingOrDefault(
//                    tenantSchema = tenantSchema,
//                    defaultName = "School Name"
//                )
//
//            println("========== SCHOOL BRANDING DEBUG neww ==========")
//            println("Tenant schema: $tenantSchema")
//            println("School name: ${schoolBranding.schoolName}")
//            println("Cloudinary logo URL: ${schoolBranding.schoolLogoUrl}")
//            println("School motto: ${schoolBranding.schoolMotto}")
//            println("========== END SCHOOL BRANDING DEBUG ==========")
//
//
//            val pdfBytes = pdfService.generateReportPack(
//                schoolName = schoolBranding.schoolName,
//                schoolLogoUrl = schoolBranding.schoolLogoUrl,
//                schoolMotto = schoolBranding.schoolMotto,
//                records = reportCards
//            )
//
//            call.response.headers.append(
//                HttpHeaders.ContentDisposition,
//                ContentDisposition.Attachment
//                    .withParameter(
//                        ContentDisposition.Parameters.FileName,
//                        "student-report-pack-$userId.pdf"
//                    )
//                    .toString()
//            )
//
//            call.respond(
//                ByteArrayContent(
//                    bytes = pdfBytes,
//                    contentType = ContentType.Application.Pdf
//                )
//            )
//        }

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

            println("some data isssss $reportCards")

            val selectedReport = reportCards.firstOrNull { record ->
                record.id == reportCardId
            }

            println("some 2 data iss $selectedReport")

            if (selectedReport == null) {
                call.respond(
                    status = HttpStatusCode.NotFound,
                    message = "Report card not found for this term"
                )
                return@get
            }

            val schoolBranding =
                call.resolveSchoolBrandingOrDefault(
                    tenantSchema = tenantSchema,
                    defaultName = "School Name"
                )

            println("========== SCHOOL BRANDING DEBUG ==========")
            println("Tenant schema: $tenantSchema")
            println("School name: ${schoolBranding.schoolName}")
            println("School logo URL thisssss: ${schoolBranding.schoolLogoUrl}")
            println("School motto: ${schoolBranding.schoolMotto}")
            println("========== END SCHOOL BRANDING DEBUG ==========")

            val pdfBytes =
                pdfService.generateReportPack(
                    schoolName = schoolBranding.schoolName,
                    schoolLogoUrl = schoolBranding.schoolLogoUrl,
                    schoolMotto = schoolBranding.schoolMotto,
                    records = listOf(selectedReport)
                )



            println("School name: ${schoolBranding.schoolName}")
            println("School logo URL thisssss: ${schoolBranding.schoolLogoUrl}")
            println("School motto: ${schoolBranding.schoolMotto}")

            println("some school data $pdfBytes")

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




private fun ApplicationCall.resolveSchoolBrandingOrDefault(
    tenantSchema: String?,
    defaultName: String = "School Name"
): SchoolBrandingInfo {

    val schoolNameHeader =
        request.header("X-School-Name")

    val schoolLogoHeader =
        request.header("X-School-Logo-Url")

    val schoolMottoHeader =
        request.header("X-School-Motto")

    val tenantSchemaHeader =
        request.header("X-Tenant-Schema")

    val tenantCodeHeader =
        request.header("X-Tenant-Code")

    val tenantSlugHeader =
        request.header("X-Tenant-Slug")

    return transaction {

        setTenantSchema("public")

        val normalizedTenantCodeHeader =
            tenantCodeHeader
                ?.takeIf { it.isNotBlank() }
                ?.let { normalizeTenantCode(it) }

        val tenantRow =
            TenantsTable
                .selectAll()
                .firstOrNull { row ->

                    val dbTenantSchema =
                        row[TenantsTable.tenantSchema]

                    val dbTenantCode =
                        row[TenantsTable.tenantCode]

                    val dbTenantSlug =
                        row[TenantsTable.tenantSlug]

                    val matchesResolvedSchema =
                        !tenantSchema.isNullOrBlank() &&
                                dbTenantSchema.equals(
                                    tenantSchema,
                                    ignoreCase = true
                                )

                    val matchesSchemaHeader =
                        !tenantSchemaHeader.isNullOrBlank() &&
                                dbTenantSchema.equals(
                                    tenantSchemaHeader,
                                    ignoreCase = true
                                )

                    val matchesTenantCode =
                        !normalizedTenantCodeHeader.isNullOrBlank() &&
                                normalizeTenantCode(dbTenantCode) == normalizedTenantCodeHeader

                    val matchesTenantSlug =
                        !tenantSlugHeader.isNullOrBlank() &&
                                dbTenantSlug.equals(
                                    tenantSlugHeader,
                                    ignoreCase = true
                                )

                    matchesResolvedSchema ||
                            matchesSchemaHeader ||
                            matchesTenantCode ||
                            matchesTenantSlug
                }

        val rawSchoolName =
            tenantRow?.get(TenantsTable.schoolName)

        val rawSchoolLogoUrl =
            tenantRow?.get(TenantsTable.schoolLogoUrl)

        val rawSchoolMotto =
            tenantRow?.get(TenantsTable.schoolMotto)

        println("========== SCHOOL BRANDING RESOLVER DEBUG ==========")
        println("tenantSchema argument: $tenantSchema")
        println("X-Tenant-Schema: $tenantSchemaHeader")
        println("X-Tenant-Code: $tenantCodeHeader")
        println("X-Tenant-Slug: $tenantSlugHeader")
        println("Tenant row found: ${tenantRow != null}")
        println("Raw school name from DB: $rawSchoolName")
        println("Raw Cloudinary logo URL from DB: $rawSchoolLogoUrl")
        println("Raw school motto from DB: $rawSchoolMotto")
        println("========== END SCHOOL BRANDING RESOLVER DEBUG ==========")

        SchoolBrandingInfo(
            schoolName =
                schoolNameHeader
                    ?.takeIf { it.isNotBlank() }
                    ?: rawSchoolName
                        ?.takeIf { it.isNotBlank() }
                    ?: defaultName,

            schoolLogoUrl =
                schoolLogoHeader
                    ?.takeIf { it.isNotBlank() }
                    ?: rawSchoolLogoUrl
                        ?.takeIf { !it.isNullOrBlank() },

            schoolMotto =
                schoolMottoHeader
                    ?.takeIf { it.isNotBlank() }
                    ?: rawSchoolMotto
                        ?.takeIf { !it.isNullOrBlank() }
        )
    }
}