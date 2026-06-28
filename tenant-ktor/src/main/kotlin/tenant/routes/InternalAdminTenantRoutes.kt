package com.example.tenant.routes

import com.example.tenant.dto.UpdateTenantStatusRequest
import com.example.tenant.dto.response.TenantStatusUpdateResponse
import com.example.tenant.dto.response.UpdateTenantStatusResponse
import com.example.tenant.repository.SuperAdminTenantRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.route

fun Route.internalSuperAdminTenantRoutes(
    internalApiKey: String
) {
    route("/superadmin/tenants") {

        get {
            val incomingKey = call.request.header("X-Internal-Api-Key")

            println("========== [TENANT INTERNAL AUTH - GET TENANTS] ==========")
            println("[TENANT] Incoming key loaded=${!incomingKey.isNullOrBlank()}")
            println("[TENANT] Incoming key length=${incomingKey?.length ?: 0}")
            println("[TENANT] Expected key loaded=${internalApiKey.isNotBlank()}")
            println("[TENANT] Expected key length=${internalApiKey.length}")
            println("==========================================================")

            if (incomingKey != internalApiKey) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("message" to "Unauthorized internal request")
                )
                return@get
            }

            val search = call.request.queryParameters["search"]
            val status = call.request.queryParameters["status"]

            val tenants = SuperAdminTenantRepository.findAllForSuperAdmin(
                search = search,
                status = status
            )

            call.respond(HttpStatusCode.OK, tenants)
        }

        patch("/{tenantCode}/status") {
            val incomingKey = call.request.header("X-Internal-Api-Key")

            println("========== [TENANT INTERNAL AUTH - UPDATE STATUS] ==========")
            println("[TENANT] Incoming key loaded=${!incomingKey.isNullOrBlank()}")
            println("[TENANT] Incoming key length=${incomingKey?.length ?: 0}")
            println("[TENANT] Expected key loaded=${internalApiKey.isNotBlank()}")
            println("[TENANT] Expected key length=${internalApiKey.length}")
            println("============================================================")

            if (incomingKey != internalApiKey) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("message" to "Unauthorized internal request")
                )
                return@patch
            }

            val tenantCode = call.parameters["tenantCode"]

            println("[TENANT] tenantCode=$tenantCode")

            if (tenantCode.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "tenantCode is required")
                )
                return@patch
            }

            val request = call.receive<UpdateTenantStatusRequest>()
            val nextStatus = request.status.trim().lowercase()

            println("[TENANT] requested status=$nextStatus")

            val allowedStatuses = setOf(
                "provisioning",
                "active",
                "inactive",
                "suspended",
                "failed"
            )

            if (nextStatus !in allowedStatuses) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Invalid tenant status: $nextStatus")
                )
                return@patch
            }

            val updated = SuperAdminTenantRepository.updateStatusByTenantCode(
                tenantCode = tenantCode,
                status = nextStatus
            )

            if (!updated) {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("message" to "Tenant not found for tenantCode: $tenantCode")
                )
                return@patch
            }

            call.respond(
                HttpStatusCode.OK,
                TenantStatusUpdateResponse(
                    success = true,
                    message = "Tenant status updated successfully"
                )
            )
        }
    }
}
