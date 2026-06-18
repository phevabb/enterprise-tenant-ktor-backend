package com.example.tenant




import com.example.tenant.dto.requests.CreateTenantRequest
import com.example.tenant.services.TenantProvisioningService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.tenantAdminModule() {
    routing {
        post("/internal/tenants/create") {
            try {
                val request = call.receive<CreateTenantRequest>()

                println("Reached /internal/tenants/create")
                println("Request payload: $request")


                val response = TenantProvisioningService.createTenant(request)
                println("Calling createTenant...")
                println("Tenant created: $response")


                call.respond(HttpStatusCode.Created, response)
            } catch (e: IllegalArgumentException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to (e.message ?: "Invalid request"))
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Unable to create tenant"))
                )
            }
        }
    }
}
