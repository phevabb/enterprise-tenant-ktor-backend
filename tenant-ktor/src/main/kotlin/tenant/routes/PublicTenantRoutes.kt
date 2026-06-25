package com.example.tenant.routes


import com.example.tenant.repository.SuperAdminTenantRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.publicTenantRoutes() {
    route("/tenants") {

        get("/by-slug/{tenantSlug}") {
            val tenantSlug = call.parameters["tenantSlug"]

            if (tenantSlug.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "tenantSlug is required")
                )
                return@get
            }

            val tenant = SuperAdminTenantRepository.findPublicTenantBySlug(
                tenantSlug = tenantSlug
            )

            if (tenant == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("message" to "Tenant not found")
                )
                return@get
            }

            call.respond(HttpStatusCode.OK, tenant)
        }
    }
}