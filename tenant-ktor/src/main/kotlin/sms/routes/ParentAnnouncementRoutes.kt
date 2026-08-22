package sms.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import sms.dto.SendParentAnnouncementRequest
import sms.dto.SendParentAnnouncementResponse
import sms.repo.SmsAnnouncementRepository
import sms.services.ParentAnnouncementService
import sms.services.SmsTenantResolver

fun Route.parentAnnouncementRoutes() {

    route("/sms/announcements") {

        post("/send") {

            try {

                println(
                    "[parent-announcement] Route hit"
                )

                val request =
                    call.receive<SendParentAnnouncementRequest>()

                println(
                    "[parent-announcement] Request=$request"
                )

                val headerTenantCode =
                    call.request.headers[
                        "X-Tenant-Code"
                    ]
                        ?.trim()
                        .orEmpty()

                val requestTenantCode =
                    request.tenantCode.trim()

                val suppliedTenantCode =
                    requestTenantCode.ifBlank {
                        headerTenantCode
                    }

                if (suppliedTenantCode.isBlank()) {

                    call.respond(
                        HttpStatusCode.BadRequest,
                        SendParentAnnouncementResponse(
                            success = false,
                            message = "Tenant code is required."
                        )
                    )

                    return@post
                }

                val resolvedTenant =
                    SmsTenantResolver.resolveByTenantCode(
                        tenantCode =
                            suppliedTenantCode
                    )

                if (resolvedTenant == null) {

                    call.respond(
                        HttpStatusCode.NotFound,
                        SendParentAnnouncementResponse(
                            success = false,
                            message =
                                "Tenant not found for code: $suppliedTenantCode"
                        )
                    )

                    return@post
                }

                println(
                    "[parent-announcement] " +
                            "Resolved tenantCode=${resolvedTenant.tenantCode}, " +
                            "tenantSchema=${resolvedTenant.tenantSchema}, " +
                            "schoolName=${resolvedTenant.schoolName}"
                )

                val safeRequest =
                    request.copy(
                        tenantCode =
                            resolvedTenant.tenantCode
                    )

                val response =
                    ParentAnnouncementService
                        .sendParentAnnouncement(
                            tenantSchema =
                                resolvedTenant.tenantSchema,

                            request =
                                safeRequest
                        )

                val announcementId =
                    SmsAnnouncementRepository
                        .saveAnnouncement(
                            schoolName =
                                resolvedTenant.schoolName,

                            request =
                                safeRequest,

                            response =
                                response
                        )

                println(
                    "[parent-announcement] " +
                            "Announcement history saved. " +
                            "announcementId=$announcementId, " +
                            "success=${response.success}"
                )

                val status =
                    when {

                        response.success -> {
                            HttpStatusCode.OK
                        }

                        response.message.contains(
                            "sender ID",
                            ignoreCase = true
                        ) -> {
                            HttpStatusCode.BadRequest
                        }

                        response.message.contains(
                            "balance",
                            ignoreCase = true
                        ) -> {
                            HttpStatusCode.PaymentRequired
                        }

                        response.message.contains(
                            "phone",
                            ignoreCase = true
                        ) -> {
                            HttpStatusCode.BadRequest
                        }

                        else -> {
                            HttpStatusCode.BadGateway
                        }
                    }

                call.respond(
                    status,
                    response
                )

            } catch (e: IllegalArgumentException) {

                println(
                    "[parent-announcement] " +
                            "Validation failed: ${e.message}"
                )

                call.respond(
                    HttpStatusCode.BadRequest,
                    SendParentAnnouncementResponse(
                        success = false,
                        message = e.message
                            ?: "Invalid announcement request."
                    )
                )

            } catch (e: Exception) {

                println(
                    "[parent-announcement] " +
                            "Failed: ${e.message}"
                )

                e.printStackTrace()

                call.respond(
                    HttpStatusCode.InternalServerError,
                    SendParentAnnouncementResponse(
                        success = false,
                        message = e.message
                            ?: "Unable to send parent announcement."
                    )
                )
            }
        }

        get("/history/{tenantCode}") {

            try {

                val suppliedTenantCode =
                    call.parameters["tenantCode"]
                        ?.trim()
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: throw IllegalArgumentException(
                            "Tenant code is required."
                        )

                val resolvedTenant =
                    SmsTenantResolver.resolveByTenantCode(
                        tenantCode =
                            suppliedTenantCode
                    )
                        ?: throw IllegalArgumentException(
                            "Tenant not found for code: $suppliedTenantCode"
                        )

                val announcements =
                    SmsAnnouncementRepository
                        .findByTenantCode(
                            tenantCode =
                                resolvedTenant.tenantCode,

                            tenantSchema =
                                resolvedTenant.tenantSchema
                        )

                call.respond(
                    HttpStatusCode.OK,
                    announcements
                )

            } catch (e: IllegalArgumentException) {

                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "message" to (
                                e.message
                                    ?: "Invalid announcement history request."
                                )
                    )
                )

            } catch (e: Exception) {

                println(
                    "[announcement-history] Failed: ${e.message}"
                )

                e.printStackTrace()

                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "message" to (
                                e.message
                                    ?: "Unable to load announcement history."
                                )
                    )
                )
            }
        }

    }
}