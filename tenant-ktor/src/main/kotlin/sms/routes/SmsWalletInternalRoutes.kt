package sms.routes


import io.ktor.http.HttpStatusCode

import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import sms.dto.CreditCashWalletRequest
import sms.dto.CreditCashWalletResponse
import sms.services.SmsWalletService

fun Route.smsWalletInternalRoutes() {

    post("/wallet/credit-cash") {

        try {

            val request =
                call.receive<CreditCashWalletRequest>()

            val response =
                SmsWalletService.creditCashWallet(
                    request = request
                )

            call.respond(
                HttpStatusCode.OK,
                response
            )

        } catch (e: IllegalArgumentException) {

            call.respond(
                HttpStatusCode.BadRequest,
                CreditCashWalletResponse(
                    success = false,
                    message = e.message ?: "Invalid wallet credit request.",
                    tenantCode = "",
                    cashBalance = "0.00",
                    smsBalance = 0
                )
            )

        } catch (e: Exception) {

            e.printStackTrace()

            call.respond(
                HttpStatusCode.InternalServerError,
                CreditCashWalletResponse(
                    success = false,
                    message = e.message ?: "Unable to credit wallet.",
                    tenantCode = "",
                    cashBalance = "0.00",
                    smsBalance = 0
                )
            )
        }
    }
}