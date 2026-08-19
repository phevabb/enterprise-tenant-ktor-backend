package com.example.tenant.routes


import com.example.tenant.repository.SuperAdminTenantRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import tenant.routes.UpdateSchoolBrandingWithoutLogoRequest




import kotlinx.serialization.Serializable
import tenant.routes.updateSchoolBrandingWithoutLogoByTenantCode


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

        put("/internal/tenants/update-school-branding-without-logo") {

            try {

                val request =
                    call.receive<UpdateSchoolBrandingWithoutLogoRequest>()

                if (request.tenantCode.isBlank()) {

                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf(
                            "error" to "Tenant code is required."
                        )
                    )
                }

                if (request.schoolName.isBlank()) {

                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf(
                            "error" to "School name is required."
                        )
                    )
                }

                val updated =
                    updateSchoolBrandingWithoutLogoByTenantCode(
                        tenantCode = request.tenantCode,
                        schoolName = request.schoolName,
                        schoolMotto = request.schoolMotto,
                        location = request.location
                    )

                if (!updated) {

                    return@put call.respond(
                        HttpStatusCode.NotFound,
                        mapOf(
                            "error" to "Tenant not found."
                        )
                    )
                }

                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "success" to true,
                        "message" to "School branding updated successfully."
                    )
                )

            } catch (e: Exception) {

                e.printStackTrace()

                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "error" to (e.message ?: "Unable to update school branding.")
                    )
                )
            }
        }
    }
}