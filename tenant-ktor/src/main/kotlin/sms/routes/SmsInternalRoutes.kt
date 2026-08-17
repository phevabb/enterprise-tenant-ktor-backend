package sms.routes



import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import sms.dto.LatestSenderIdResponse
import sms.dto.RequestSenderIdRequest
import sms.dto.SenderIdMessageResponse
import sms.services.SenderIdAlreadyExistsException
import sms.services.SmsSenderIdService

fun Route.smsInternalRoutes() {

    delete("/sender-id/{id}") {

        try {

            val id =
                call.parameters["id"]
                    ?.toIntOrNull()
                    ?: throw IllegalArgumentException(
                        "Invalid sender ID request id."
                    )

            val tenantCode =
                call.request.headers["X-Tenant-Code"]
                    ?: throw IllegalArgumentException(
                        "Tenant code is required."
                    )

            val deleted =
                SmsSenderIdService.deleteSenderIdForTenant(
                    id = id,
                    tenantCode = tenantCode
                )

            if (!deleted) {

                call.respond(
                    HttpStatusCode.NotFound,
                    SenderIdMessageResponse(
                        success = false,
                        message = "Sender ID request not found."
                    )
                )

                return@delete
            }

            call.respond(
                HttpStatusCode.OK,
                SenderIdMessageResponse(
                    success = true,
                    message = "Sender ID request deleted successfully."
                )
            )

        } catch (e: IllegalArgumentException) {

            call.respond(
                HttpStatusCode.BadRequest,
                SenderIdMessageResponse(
                    success = false,
                    message = e.message
                        ?: "Invalid delete request."
                )
            )

        } catch (e: Exception) {

            e.printStackTrace()

            call.respond(
                HttpStatusCode.InternalServerError,
                SenderIdMessageResponse(
                    success = false,
                    message = e.message
                        ?: "Unable to delete sender ID request."
                )
            )
        }
    }

    post("/sender-id/request") {

        try {

            val request =
                call.receive<RequestSenderIdRequest>()

            val response =
                SmsSenderIdService.requestSenderId(
                    request
                )

            call.respond(
                HttpStatusCode.Created,
                response
            )

        } catch (e: SenderIdAlreadyExistsException) {

            call.respond(
                HttpStatusCode.Conflict,
                SenderIdMessageResponse(
                    success = false,
                    message = e.message
                        ?: "Sender ID already exists. Delete previous one to create a new one."
                )
            )

        } catch (e: IllegalArgumentException) {

            call.respond(
                HttpStatusCode.BadRequest,
                SenderIdMessageResponse(
                    success = false,
                    message = e.message
                        ?: "Invalid sender ID request."
                )
            )

        } catch (e: Exception) {

            e.printStackTrace()

            call.respond(
                HttpStatusCode.InternalServerError,
                SenderIdMessageResponse(
                    success = false,
                    message = e.message
                        ?: "Unable to request sender ID."
                )
            )
        }
    }


    get("/sender-id/latest/{tenantCode}") {

        try {

            val tenantCode =
                call.parameters["tenantCode"]
                    ?: throw IllegalArgumentException(
                        "Tenant code is required."
                    )

            val senderId =
                SmsSenderIdService.findLatestForTenant(
                    tenantCode
                )

            if (senderId == null) {

                call.respond(
                    HttpStatusCode.OK,
                    LatestSenderIdResponse(
                        success = true,
                        available = false,
                        message = "No sender ID available.",
                        senderId = null
                    )
                )

                return@get
            }

            call.respond(
                HttpStatusCode.OK,
                LatestSenderIdResponse(
                    success = true,
                    available = true,
                    message = "Sender ID found.",
                    senderId = senderId
                )
            )

        } catch (e: IllegalArgumentException) {

            call.respond(
                HttpStatusCode.BadRequest,
                LatestSenderIdResponse(
                    success = false,
                    available = false,
                    message = e.message ?: "Invalid request.",
                    senderId = null
                )
            )

        } catch (e: Exception) {

            e.printStackTrace()

            call.respond(
                HttpStatusCode.InternalServerError,
                LatestSenderIdResponse(
                    success = false,
                    available = false,
                    message = e.message ?: "Unable to retrieve sender ID.",
                    senderId = null
                )
            )
        }
    }
}