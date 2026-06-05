package com.example.staff.routes


import com.example.staff.dtos.requests.CreateStaffRequest
import com.example.staff.dtos.requests.PatchStaffRequest
import com.example.staff.repos.StaffAssignedClassRepository
import com.example.staff.repos.StaffRepository
import com.example.staff.services.StaffService
import com.example.student.repos.StudentRepository
import com.example.tenant.currentTenant

import io.ktor.http.*
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate

import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlin.text.get


private fun Routing.getTenantSchema(call: ApplicationCall): String {
    return call.currentTenant().tenantSchema
}

fun Route.staffRoutes() {


    authenticate("auth-jwt") {


        // ✅ RAW list
        get("/raw") {

            println("========== STAFF RAW REQUEST START ==========")

            val search = call.request.queryParameters["search"]
            println("Query param search = $search")

            val tenant = call.currentTenant()

            println("Tenant object = $tenant")

            if (tenant == null) {
                println("❌ Tenant is NULL - request rejected before DB access")
                call.respond(HttpStatusCode.NotFound, "Tenant not found")
                return@get
            }

            val tenantSchema = tenant.tenantSchema
            println("Resolved tenantSchema = $tenantSchema")

            try {
                val staff = StaffRepository.findAllWithUserAndClass(
                    tenantSchema = tenantSchema,
                    search = search
                )

                println("Staff result size = ${staff.size}")
                println("========== STAFF RAW REQUEST SUCCESS ==========")

                call.respond(HttpStatusCode.OK, staff)

            } catch (e: Exception) {
                println("❌ ERROR IN STAFF RAW ENDPOINT")
                println("Message: ${e.message}")
                e.printStackTrace()

                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "unknown error"))
                )
            }
        }


        // ✅ CREATE
        post {
            val req = call.receive<CreateStaffRequest>()
            println("staff request => $req")

            val tenantSchema = call.currentTenant().tenantSchema
            val created = StaffService.createStaff(tenantSchema, req)

            call.respond(HttpStatusCode.Created, created)
        }

        // ✅ PATCH (✅ THIS IS WHAT YOU WANTED)
        patch("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()

            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid id"))
                return@patch
            }

            val req = call.receive<PatchStaffRequest>()

            val tenantSchema = call.currentTenant().tenantSchema

            val updated = StaffRepository.patchNested(
                tenantSchema = tenantSchema,
                id = id,
                req = req
            )

            if (updated == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Staff not found"))
            } else {
                call.respond(HttpStatusCode.OK, updated)
            }
        }

        // ✅ DELETE
        delete("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()

            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid id")
                return@delete
            }

            val tenantSchema = call.currentTenant().tenantSchema
            val ok = StaffService.deleteStaff(tenantSchema, id)

            if (!ok) {
                call.respond(HttpStatusCode.NotFound, "Staff not found")
            } else {
                call.respond(HttpStatusCode.NoContent)
            }
        }

        get("/teacher-students") {
            try {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "Invalid token")
                    )

                val userId = principal.payload.getClaim("userId").asInt()
                val role = principal.payload.getClaim("role").asString()

                if (role != "staff") {
                    return@get call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "Only staff can access this endpoint")
                    )
                }

                val tenantSchema = call.currentTenant().tenantSchema

                val staff = StaffRepository.findByUserId(
                    tenantSchema = tenantSchema,
                    userId = userId
                ) ?: return@get call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf("error" to "Staff profile not found")
                )

                val classId = staff.assignedClassId
                    ?: return@get call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "Teacher has no assigned class")
                    )

                val students = StudentRepository.findStudentsByClass(
                    tenantSchema = tenantSchema,
                    classId = classId
                )

                call.respond(HttpStatusCode.OK, students)

            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message)
                )
            }
        }

        get("assigned-class/{userId}") {

            val tenant = call.currentTenant()
            val userId = call.parameters["userId"]?.trim()

            if (userId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "userId is required"))
                return@get
            }



            val result = StaffAssignedClassRepository.findAssignedClassByUserId(
                tenantSchema = tenant.tenantSchema,

                userId = userId
            )

            if (result == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Staff profile not found for userId"))
            } else {
                call.respond(HttpStatusCode.OK, result)
            }
        }






    }

}






