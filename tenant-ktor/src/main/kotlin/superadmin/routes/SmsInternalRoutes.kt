package superadmin.routes



import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.put

import sms.dto.SenderIdMessageResponse

import superadmin.dto.requests.RejectSenderIdRequest
import superadmin.service.SmsSenderIdService

fun Route.smsInternalRoutes() {

    get("/sender-id/all") {

        try {

            val response =
                SmsSenderIdService.findAll()

            call.respond(
                HttpStatusCode.OK,
                response
            )

        } catch (e: Exception) {

            e.printStackTrace()

            call.respond(
                HttpStatusCode.InternalServerError,
                SenderIdMessageResponse(
                    success = false,
                    message = e.message
                        ?: "Unable to retrieve sender ID requests."
                )
            )
        }
    }

    put("/sender-id/{id}/approve") {

        try {

            val id =
                call.parameters["id"]
                    ?.toIntOrNull()
                    ?: throw IllegalArgumentException(
                        "Invalid sender ID request id."
                    )

            val response =
                SmsSenderIdService.approveSenderId(
                    id = id
                )

            call.respond(
                HttpStatusCode.OK,
                response
            )

        } catch (e: IllegalArgumentException) {

            call.respond(
                HttpStatusCode.BadRequest,
                SenderIdMessageResponse(
                    success = false,
                    message = e.message
                        ?: "Invalid approval request."
                )
            )

        } catch (e: Exception) {

            e.printStackTrace()

            call.respond(
                HttpStatusCode.InternalServerError,
                SenderIdMessageResponse(
                    success = false,
                    message = e.message
                        ?: "Unable to approve sender ID."
                )
            )
        }
    }

    put("/sender-id/{id}/reject") {

        try {

            val id =
                call.parameters["id"]
                    ?.toIntOrNull()
                    ?: throw IllegalArgumentException(
                        "Invalid sender ID request id."
                    )

            val request =
                call.receive<RejectSenderIdRequest>()

            val response =
                SmsSenderIdService.rejectSenderId(
                    id = id,
                    rejectionReason = request.rejectionReason
                )

            call.respond(
                HttpStatusCode.OK,
                response
            )

        } catch (e: IllegalArgumentException) {

            call.respond(
                HttpStatusCode.BadRequest,
                SenderIdMessageResponse(
                    success = false,
                    message = e.message
                        ?: "Invalid rejection request."
                )
            )

        } catch (e: Exception) {

            e.printStackTrace()

            call.respond(
                HttpStatusCode.InternalServerError,
                SenderIdMessageResponse(
                    success = false,
                    message = e.message
                        ?: "Unable to reject sender ID."
                )
            )
        }
    }

    delete("/sender-id/{id}") {

        try {

            val id =
                call.parameters["id"]
                    ?.toIntOrNull()
                    ?: throw IllegalArgumentException(
                        "Invalid sender ID request id."
                    )

            val deleted =
                SmsSenderIdService.deleteSenderId(
                    id = id
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
}