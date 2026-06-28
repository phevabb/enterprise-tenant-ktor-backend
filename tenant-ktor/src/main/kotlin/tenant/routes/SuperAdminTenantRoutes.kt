package com.example.tenant.routes



import com.example.tenant.dto.requests.UpdateTenantStatusRequest
import com.example.tenant.dto.response.UpdateTenantStatusResponse
import com.example.tenant.repository.SuperAdminTenantRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.patch
import io.ktor.server.routing.route

fun Route.superAdminTenantRoutes() {
    route("superadmin/tenants") {

        patch("/{tenantCode}/status") {
            val tenantCode = call.parameters["tenantCode"]

            if (tenantCode.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "tenantCode is required")
                )
                return@patch
            }

            val request = call.receive<UpdateTenantStatusRequest>()

            val nextStatus = request.status.trim().lowercase()

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
                UpdateTenantStatusResponse(
                    message = "Tenant status updated successfully",
                    tenantCode = tenantCode,
                    status = nextStatus
                )
            )
        }
    }
}