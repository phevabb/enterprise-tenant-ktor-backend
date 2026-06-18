package com.example.tenant.routes



import com.example.tenant.currentTenant
import com.example.tenant.dto.response.TenantResponse
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.tenantRoutes() {



        get("/me") {
            val tenant = call.currentTenant()

            val response = TenantResponse(
                schoolName = tenant.schoolName,
                tenantCode = tenant.tenantCode,
                tenantSlug = tenant.tenantSlug
            )

            call.respond(response)
        }

}