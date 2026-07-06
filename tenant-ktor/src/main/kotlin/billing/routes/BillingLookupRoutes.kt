package com.example.billing.routes





import com.example.billing.repos.BillingLookupRepository
import com.example.billing.repos.resolveTenantSchemaOrNull
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.billingLookupRoutes() {
    route("/api/billing/lookups") {

        /**
         * Get all lookup data needed by the billing template form.
         *
         * GET /api/billing/lookups
         */
        get {
            val tenantSchema = call.requireLookupTenantSchema() ?: return@get

            val lookups = BillingLookupRepository.findBillingTemplateLookups(
                tenantSchema = tenantSchema
            )

            call.respond(HttpStatusCode.OK, lookups)
        }

        /**
         * Get categories.
         *
         * GET /api/billing/lookups/categories
         */
        get("/categories") {
            val tenantSchema = call.requireLookupTenantSchema() ?: return@get

            val categories = BillingLookupRepository.findCategories(
                tenantSchema = tenantSchema
            )

            call.respond(HttpStatusCode.OK, categories)
        }

        /**
         * Get academic years.
         *
         * GET /api/billing/lookups/academic-years
         */
        get("/academic-years") {
            val tenantSchema = call.requireLookupTenantSchema() ?: return@get

            val years = BillingLookupRepository.findAcademicYears(
                tenantSchema = tenantSchema
            )

            call.respond(HttpStatusCode.OK, years)
        }

        /**
         * Get terms.
         *
         * Optional:
         * GET /api/billing/lookups/terms?academicYearId=1
         *
         * GET /api/billing/lookups/terms
         */
        get("/terms") {
            val tenantSchema = call.requireLookupTenantSchema() ?: return@get

            val academicYearId = call.request.queryParameters["academicYearId"]
                ?.toIntOrNull()

            val terms = BillingLookupRepository.findTerms(
                tenantSchema = tenantSchema,
                academicYearId = academicYearId
            )

            call.respond(HttpStatusCode.OK, terms)
        }

        /**
         * Get current academic year and current term.
         *
         * GET /api/billing/lookups/current
         */
        get("/current") {
            val tenantSchema = call.requireLookupTenantSchema() ?: return@get

            val currentYear = BillingLookupRepository.findCurrentAcademicYear(
                tenantSchema = tenantSchema
            )

            val currentTerm = BillingLookupRepository.findCurrentTerm(
                tenantSchema = tenantSchema
            )

            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "currentAcademicYear" to currentYear,
                    "currentAcademicTerm" to currentTerm
                )
            )
        }
    }
}

private suspend fun ApplicationCall.requireLookupTenantSchema(): String? {
    val tenantSchema = resolveTenantSchemaOrNull()

    if (tenantSchema.isNullOrBlank()) {
        respond(
            HttpStatusCode.BadRequest,
            mapOf("message" to "Tenant schema could not be resolved.")
        )

        return null
    }

    return tenantSchema
}


