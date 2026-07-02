package com.example.superadmin.routes

import com.example.superadmin.repo.SuperAdminBillingTenantRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.superRoutes() {
    route("/tenants") {

        /**
         * GET /api/internal/superadmin/tenants/billing-summary
         *
         * Header:
         * X-Internal-Api-Key: change-this-secret-key
         */
        get("/billing-summary") {
            val incomingKey = call.request.header("X-Internal-Api-Key")

            val internalApiKey = System.getenv("TENANT_INTERNAL_API_KEY")
                ?: "change-this-secret-key"

            println("========== [TENANT INTERNAL AUTH - BILLING SUMMARY] ==========")
            println("[TENANT] Incoming key loaded=${!incomingKey.isNullOrBlank()}")
            println("[TENANT] Incoming key length=${incomingKey?.length ?: 0}")
            println("[TENANT] Expected key loaded=${internalApiKey.isNotBlank()}")
            println("[TENANT] Expected key length=${internalApiKey.length}")
            println("==============================================================")

            if (incomingKey?.trim() != internalApiKey.trim()) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("message" to "Unauthorized internal request")
                )
                return@get
            }

            val search = call.request.queryParameters["search"]
            val status = call.request.queryParameters["status"]

            val tenants = SuperAdminBillingTenantRepository.findAllForSuperAdminBilling(
                search = search,
                status = status
            )

            call.respond(HttpStatusCode.OK, tenants)
        }
    }
}