package sms.routes


import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import sms.dto.SmsBalanceErrorResponse
import sms.services.ClientSmsBalanceService

fun Route.clientSmsBalanceRoutes() {

    get(
        "/sms/wallet/balance/by-tenant/{tenantCode}"
    ) {

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
                ClientSmsBalanceService
                    .getSmsBalanceByTenantCode(
                        tenantCode = tenantCode
                    )

            call.respond(
                HttpStatusCode.OK,
                response
            )

        } catch (e: IllegalArgumentException) {

            call.respond(
                HttpStatusCode.BadRequest,
                SmsBalanceErrorResponse(
                    success = false,
                    message = e.message
                        ?: "Invalid SMS balance request."
                )
            )

        } catch (e: Exception) {

            println(
                "[client-sms-balance] Failed: ${e.message}"
            )

            e.printStackTrace()

            call.respond(
                HttpStatusCode.InternalServerError,
                SmsBalanceErrorResponse(
                    success = false,
                    message = e.message
                        ?: "Unable to retrieve SMS balance."
                )
            )
        }
    }
}