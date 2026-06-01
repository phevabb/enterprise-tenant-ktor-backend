package com.example.admin.routes



import com.example.admin.dtos.requests.CreateAdminRequest
import com.example.admin.dtos.requests.PatchAdminRequest
import com.example.admin.repos.AdminRepository
import com.example.admin.services.AdminService


import io.ktor.http.*
import io.ktor.server.auth.authenticate
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.adminRoutes() {
    authenticate("auth-jwt") {
    // ✅ RAW list
    get("/raw") {
        val search = call.request.queryParameters["search"]
        val admin = AdminRepository.findAllWithUserAndClass(search)
        call.respond(HttpStatusCode.OK, admin)
    }

    // ✅ CREATE
    post {
        val req = call.receive<CreateAdminRequest>()
        println("admin request => $req")

        val created = AdminService.createAdmin(req)

        call.respond(HttpStatusCode.Created, created)
    }

    // ✅ PATCH (✅ THIS IS WHAT YOU WANTED)
        patch("{id}") {
            println("PATCH /admin/{id} called")

            val rawId = call.parameters["id"]
            println("Raw id from path: $rawId")

            val id = rawId?.toIntOrNull()
            println("Parsed id: $id")

            if (id == null) {
                println("Invalid id received: $rawId")
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid id"))
                return@patch
            }

            try {
                val req = call.receive<PatchAdminRequest>()
                println("Received PatchAdminRequest: $req")

                val updated = AdminRepository.patchNested(id, req)
                println("Repository result for id=$id: $updated")

                if (updated == null) {
                    println("Admin not found for id=$id")
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Admin not found"))
                } else {
                    println("Admin updated successfully for id=$id")
                    call.respond(HttpStatusCode.OK, updated)
                }
            } catch (e: Exception) {
                println("Error while patching admin id=$id: ${e.message}")
                e.printStackTrace()

                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to ("Failed to patch admin: ${e.message}"))
                )
            }
        }

    // ✅ DELETE
    delete("{id}") {
        val id = call.parameters["id"]?.toIntOrNull()

        if (id == null) {
            call.respond(HttpStatusCode.BadRequest, "Invalid id")
            return@delete
        }

        val ok = AdminService.deleteAdmin(id)

        if (!ok) {
            call.respond(HttpStatusCode.NotFound, "Admin not found")
        } else {
            call.respond(HttpStatusCode.NoContent)
        }
    }

}
}
