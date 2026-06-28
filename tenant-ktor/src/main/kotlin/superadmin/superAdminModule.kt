package com.example.superadmin






import com.example.tenant.routes.internalSuperAdminTenantRoutes
import com.example.tenant.routes.superAdminTenantRoutes
import com.example.tenant.routes.tenantRoutes
import io.ktor.server.application.*
import io.ktor.server.routing.*



fun Application.superAdminModule() {
    val INTERNAL_API_KEY = "change-this-to-a-long-random-secret"
    routing {
        route("/api") {

            route("/internal") {
                internalSuperAdminTenantRoutes(INTERNAL_API_KEY)
            }

            route("/") {
                superAdminTenantRoutes()
            }




        }
    }
}