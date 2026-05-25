package com.example.staff.routes


import com.example.staff.dtos.requests.CreateStaffRequest
import com.example.staff.dtos.requests.PatchStaffRequest
import com.example.staff.repos.StaffAssignedClassRepository
import com.example.staff.repos.StaffRepository
import com.example.staff.services.StaffService
import com.example.student.repos.StudentRepository

import io.ktor.http.*
import io.ktor.server.auth.authenticate

import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlin.text.get

fun Route.staffRoutes() {
    authenticate("auth-jwt") {
        // ✅ RAW list
        get("/raw") {
            val search = call.request.queryParameters["search"]
            val staff = StaffRepository.findAllWithUserAndClass(search)
            call.respond(HttpStatusCode.OK, staff)
        }


        // ✅ CREATE
        post {
            val req = call.receive<CreateStaffRequest>()
            println("staff request => $req")

            val created = StaffService.createStaff(req)

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
            val updated = StaffRepository.patchNested(id, req)

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

            val ok = StaffService.deleteStaff(id)

            if (!ok) {
                call.respond(HttpStatusCode.NotFound, "Staff not found")
            } else {
                call.respond(HttpStatusCode.NoContent)
            }
        }

        get("/teacher-students") {

            try {
                val principal = call.principal<JWTPrincipal>()
                if (principal == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))
                    return@get
                }

                val userId = principal.payload.getClaim("userId").asInt()
                val role = principal.payload.getClaim("role").asString()

                // ✅ ONLY allow staff
                if (role != "staff") {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "Only staff can access this endpoint")
                    )
                    return@get
                }

                // ✅ 1. Get staff profile
                val staff = StaffRepository.findByUserId(userId)

                if (staff == null) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "Staff profile not found")
                    )
                    return@get
                }

                // ✅ 2. Get assigned class
                val classId = staff.assignedClassId

                if (classId == null) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        mapOf("error" to "Teacher has no assigned class")
                    )
                    return@get
                }

                // ✅ 3. Get students in that class
                val students = StudentRepository.findStudentsByClass(classId)

                // ✅ 4. Return result
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
            val userId = call.parameters["userId"]?.trim()
            if (userId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "userId is required"))
                return@get
            }

            val result = StaffAssignedClassRepository.findAssignedClassByUserId(userId)

            if (result == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Staff profile not found for userId"))
            } else {
                call.respond(HttpStatusCode.OK, result)
            }
        }






    }

}






