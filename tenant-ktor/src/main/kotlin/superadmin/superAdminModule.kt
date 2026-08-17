package com.example.superadmin






import com.example.superadmin.routes.superAdminTransactionRoutes
import com.example.superadmin.routes.superRoutes
import com.example.tenant.routes.internalSuperAdminTenantRoutes
import com.example.tenant.routes.superAdminTenantRoutes
import com.example.tenant.routes.tenantRoutes
import io.ktor.server.application.*
import io.ktor.server.routing.*
import sms.routes.smsWalletInternalRoutes
import superadmin.routes.smsAdminWalletInternalRoutes
import superadmin.routes.smsInternalRoutes


fun Application.superAdminModule() {
    val INTERNAL_API_KEY = "change-this-secret-key"
    routing {
        route("/api") {

            route("/internal") {
                internalSuperAdminTenantRoutes(INTERNAL_API_KEY)
            }

            route("/") {
                superAdminTenantRoutes()
            }

            route("/internal/superadmin") {
                superRoutes()
            }

            route("/internal/sms") {
                smsInternalRoutes()
                smsAdminWalletInternalRoutes()

            }



                route("/internal/super/transactions") {
                    superAdminTransactionRoutes()
                }

                // existing routes...







        }
    }
}