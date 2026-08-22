package sms.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import sms.services.ClientSenderIdService

@Serializable
private data class ClientSenderIdErrorResponse(
    val success: Boolean,
    val message: String
)

fun Route.clientSenderIdRoutes() {

    route("/sms/sender-id") {

        get("/by-tenant/{tenantCode}") {

            try {

                val tenantCode =
                    call.parameters["tenantCode"]
                        ?.trim()
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: throw IllegalArgumentException(
                            "Tenant code is required."
                        )

                val response =
                    ClientSenderIdService
                        .getLatestSenderIdByTenantCode(
                            tenantCode = tenantCode
                        )

                call.respond(
                    HttpStatusCode.OK,
                    response
                )

            } catch (e: IllegalArgumentException) {

                call.respond(
                    HttpStatusCode.BadRequest,
                    ClientSenderIdErrorResponse(
                        success = false,
                        message = e.message
                            ?: "Invalid sender ID request."
                    )
                )

            } catch (e: Exception) {

                println(
                    "[client-sender-id] Unable to retrieve sender ID: ${e.message}"
                )

                e.printStackTrace()

                call.respond(
                    HttpStatusCode.InternalServerError,
                    ClientSenderIdErrorResponse(
                        success = false,
                        message = e.message
                            ?: "Unable to retrieve sender ID."
                    )
                )
            }
        }
    }
}