package sms.routes


import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import sms.dto.SenderIdMessageResponse
import sms.services.SmsWalletClientService

fun Route.smsWalletClientInternalRoutes() {

    get("/wallet/{tenantCode}") {

        try {

            val tenantCode =
                call.parameters["tenantCode"]
                    ?: throw IllegalArgumentException(
                        "Tenant code is required."
                    )

            val wallet =
                SmsWalletClientService.getOrCreateWalletByTenantCode(
                    tenantCode = tenantCode
                )

            call.respond(
                HttpStatusCode.OK,
                wallet
            )

        } catch (e: IllegalArgumentException) {

            call.respond(
                HttpStatusCode.BadRequest,
                SenderIdMessageResponse(
                    success = false,
                    message = e.message
                        ?: "Invalid wallet request."
                )
            )

        } catch (e: Exception) {

            e.printStackTrace()

            call.respond(
                HttpStatusCode.InternalServerError,
                SenderIdMessageResponse(
                    success = false,
                    message = e.message
                        ?: "Unable to retrieve wallet."
                )
            )
        }
    }
}