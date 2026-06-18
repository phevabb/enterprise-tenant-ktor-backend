package com.example.student.routes

import com.example.student.repos.NewGradeClassRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.example.student.dtos.requests.CreateNewGradeClassRequest
import com.example.student.dtos.requests.PatchNewGradeClassRequest
import com.example.tenant.currentTenant
import io.ktor.server.auth.authenticate

fun Route.gradeClassRoutes() {

    authenticate("auth-jwt") {
        get {

            val tenant = call.currentTenant()

            val gradeclasses = NewGradeClassRepository.findAll(
                tenantSchema = tenant.tenantSchema
            )

            call.respond(HttpStatusCode.OK, gradeclasses)
        }

        post {

            val tenant = call.currentTenant()

            val req = call.receive<CreateNewGradeClassRequest>()

            val created = NewGradeClassRepository.create(
                tenantSchema = tenant.tenantSchema,
                name = req.name.trim(),
                categoryId = req.categoryId,
                isActive = true
            )

            call.respond(HttpStatusCode.Created, created)
        }

        delete("{id}") {

            val tenant = call.currentTenant()

            val id = call.parameters["id"]?.toIntOrNull()

            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "invalid id")
                return@delete
            }

            val ok = NewGradeClassRepository.delete(
                tenantSchema = tenant.tenantSchema,
                id = id
            )

            if (!ok) {
                call.respond(HttpStatusCode.NotFound, "Class not found")
            } else {
                call.respond(HttpStatusCode.NoContent)
            }
        }

        patch("{id}") {

            val tenant = call.currentTenant()

            val id = call.parameters["id"]?.toIntOrNull()

            println("id is $id")

            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "invalid id")
                return@patch
            }

            val req = call.receive<PatchNewGradeClassRequest>()

            val updated = NewGradeClassRepository.patch(
                tenantSchema = tenant.tenantSchema,
                id = id,
                req = req
            )

            if (updated == null) {
                call.respond(HttpStatusCode.NotFound, "invalid id")
            } else {
                call.respond(HttpStatusCode.OK, updated)
            }
        }
    }
}