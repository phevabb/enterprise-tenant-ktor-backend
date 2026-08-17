package superadmin.routes



import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import sms.dto.SenderIdMessageResponse
import superadmin.service.SmsAdminWalletService

fun Route.smsAdminWalletInternalRoutes() {

    get("/wallets") {

        try {

            val wallets =
                SmsAdminWalletService.getAllWallets()

            call.respond(
                HttpStatusCode.OK,
                wallets
            )

        } catch (e: Exception) {

            e.printStackTrace()

            call.respond(
                HttpStatusCode.InternalServerError,
                SenderIdMessageResponse(
                    success = false,
                    message = e.message
                        ?: "Unable to retrieve SMS wallets."
                )
            )
        }
    }

    get("/wallets/{tenantCode}") {

        try {

            val tenantCode =
                call.parameters["tenantCode"]
                    ?: throw IllegalArgumentException(
                        "Tenant code is required."
                    )

            val wallet =
                SmsAdminWalletService.getWalletByTenantCode(
                    tenantCode = tenantCode
                )

            if (wallet == null) {

                call.respond(
                    HttpStatusCode.NotFound,
                    SenderIdMessageResponse(
                        success = false,
                        message = "Wallet not found for this tenant."
                    )
                )

                return@get
            }

            call.respond(
                HttpStatusCode.OK,
                wallet
            )

        } catch (e: IllegalArgumentException) {

            call.respond(
                HttpStatusCode.BadRequest,
                SenderIdMessageResponse(
                    success = false,
                    message = e.message ?: "Invalid wallet request."
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



    get("/wallet-transactions") {

        try {

            val transactions =
                SmsAdminWalletService.getAllWalletTransactions()

            call.respond(
                HttpStatusCode.OK,
                transactions
            )

        } catch (e: Exception) {

            e.printStackTrace()

            call.respond(
                HttpStatusCode.InternalServerError,
                SenderIdMessageResponse(
                    success = false,
                    message = e.message
                        ?: "Unable to retrieve wallet transactions."
                )
            )
        }
    }

    get("/wallet-transactions/{tenantCode}") {

        try {

            val tenantCode =
                call.parameters["tenantCode"]
                    ?: throw IllegalArgumentException(
                        "Tenant code is required."
                    )

            val transactions =
                SmsAdminWalletService.getWalletTransactionsByTenantCode(
                    tenantCode = tenantCode
                )

            call.respond(
                HttpStatusCode.OK,
                transactions
            )

        } catch (e: IllegalArgumentException) {

            call.respond(
                HttpStatusCode.BadRequest,
                SenderIdMessageResponse(
                    success = false,
                    message = e.message ?: "Invalid transaction request."
                )
            )

        } catch (e: Exception) {

            e.printStackTrace()

            call.respond(
                HttpStatusCode.InternalServerError,
                SenderIdMessageResponse(
                    success = false,
                    message = e.message
                        ?: "Unable to retrieve wallet transactions."
                )
            )
        }
    }
}