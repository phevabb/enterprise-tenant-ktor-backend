package com.example.admin.routes

import com.example.admin.dtos.requests.CreateAdminRequest
import com.example.admin.services.AdminService
import com.example.tenant.currentTenant
import com.example.tenant.tenantTransaction
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import com.example.admin.dtos.requests.PatchAdminRequest
import com.example.admin.repos.AdminRepository


fun Route.adminRoutes() {
    authenticate("auth-jwt") {

        get("/raw") {
            val tenant = call.currentTenant()
            val search = call.request.queryParameters["search"]

            val admins = tenantTransaction(tenant.tenantSchema) {
                AdminRepository.findAllWithUserAndClassInCurrentTransaction(search)
            }

            call.respond(HttpStatusCode.OK, admins)
        }

        post {
            val tenant = call.currentTenant()
            val req = call.receive<CreateAdminRequest>()

            val created = AdminService.createAdmin(
                tenantSchema = tenant.tenantSchema,
                request = req
            )

            call.respond(HttpStatusCode.Created, created)
        }

        patch("{id}") {
            val tenant = call.currentTenant()
            val id = call.parameters["id"]?.toIntOrNull()

            if (id == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Invalid id")
                )
                return@patch
            }

            try {
                val req = call.receive<PatchAdminRequest>()

                val updated = tenantTransaction(tenant.tenantSchema) {
                    AdminRepository.patchNestedInCurrentTransaction(id, req)
                }

                if (updated == null) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Admin not found")
                    )
                } else {
                    call.respond(HttpStatusCode.OK, updated)
                }
            } catch (e: Exception) {
                e.printStackTrace()

                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Failed to patch admin: ${e.message}")
                )
            }
        }

        delete("{id}") {
            val tenant = call.currentTenant()
            val id = call.parameters["id"]?.toIntOrNull()

            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid id")
                return@delete
            }

            val ok = AdminService.deleteAdmin(
                tenantSchema = tenant.tenantSchema,
                id = id
            )

            if (!ok) {
                call.respond(HttpStatusCode.NotFound, "Admin not found")
            } else {
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
