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





import tenant.repository.TenantBrandingRepository.updateSchoolBrandingWithoutLogoByTenantCode


import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import tenant.dto.response.SchoolBrandingUpdateResponse
import tenant.repository.TenantBrandingRepository


fun Route.publicTenantRoutes() {

    route("/tenants") {

        get("/by-slug/{tenantSlug}") {

            val tenantSlug =
                call.parameters["tenantSlug"]

            if (tenantSlug.isNullOrBlank()) {

                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "message" to "tenantSlug is required"
                    )
                )

                return@get
            }

            val tenant =
                SuperAdminTenantRepository.findPublicTenantBySlug(
                    tenantSlug = tenantSlug
                )

            if (tenant == null) {

                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf(
                        "message" to "Tenant not found"
                    )
                )

                return@get
            }

            call.respond(
                HttpStatusCode.OK,
                tenant
            )
        }
    }

    put(
        "/internal/tenants/update-school-branding-without-logo"
    ) {

        try {

            println(
                "[update-school-branding-without-logo] Route hit"
            )

            println(
                "[update-school-branding-without-logo] Content-Type = ${
                    call.request.headers["Content-Type"]
                }"
            )

            val request =
                call.receive<UpdateSchoolBrandingWithoutLogoRequest>()

            println(
                "[update-school-branding-without-logo] Request=$request"
            )

            if (request.tenantCode.isBlank()) {

                call.respond(
                    HttpStatusCode.BadRequest,
                    SchoolBrandingUpdateResponse(
                        success = false,
                        message = "Tenant code is required."
                    )
                )

                return@put
            }

            if (request.schoolName.isBlank()) {

                call.respond(
                    HttpStatusCode.BadRequest,
                    SchoolBrandingUpdateResponse(
                        success = false,
                        message = "School name is required."
                    )
                )

                return@put
            }

            val updated =
                TenantBrandingRepository
                    .updateSchoolBrandingWithoutLogoByTenantCode(
                        tenantCode = request.tenantCode,
                        schoolName = request.schoolName,
                        schoolMotto = request.schoolMotto,
                        location = request.location
                    )

            println(
                "[update-school-branding-without-logo] Updated=$updated"
            )

            if (!updated) {

                call.respond(
                    HttpStatusCode.NotFound,
                    SchoolBrandingUpdateResponse(
                        success = false,
                        message =
                            "Tenant not found for code: ${request.tenantCode}"
                    )
                )

                return@put
            }

            call.respond(
                HttpStatusCode.OK,
                SchoolBrandingUpdateResponse(
                    success = true,
                    message = "School branding updated successfully."
                )
            )

        } catch (e: IllegalArgumentException) {

            println(
                "[update-school-branding-without-logo] " +
                        "Validation failed: ${e.message}"
            )

            call.respond(
                HttpStatusCode.BadRequest,
                SchoolBrandingUpdateResponse(
                    success = false,
                    message = e.message
                        ?: "Invalid school branding request."
                )
            )

        } catch (e: Exception) {

            println(
                "[update-school-branding-without-logo] " +
                        "Failed: ${e.message}"
            )

            e.printStackTrace()

            call.respond(
                HttpStatusCode.InternalServerError,
                SchoolBrandingUpdateResponse(
                    success = false,
                    message = e.message
                        ?: "Unable to update school branding."
                )
            )
        }
    }
}
