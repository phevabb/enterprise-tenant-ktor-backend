package com.example.student.routes

import com.example.student.repos.NewGradeClassRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.example.student.dtos.requests.CreateNewGradeClassRequest
import com.example.student.dtos.requests.PatchNewGradeClassRequest
import io.ktor.server.auth.authenticate

fun Route.gradeClassRoutes() {

    authenticate("auth-jwt") {
        get {
            val gradeclasses = NewGradeClassRepository.findAll()

            call.respond(HttpStatusCode.OK, gradeclasses)
        }



        post {

            val req = call.receive<CreateNewGradeClassRequest>()

            val created = NewGradeClassRepository.create(
                name = req.name.trim(),
                categoryId = req.categoryId,
                isActive = true

            )

            call.respond(HttpStatusCode.Created, created)
        }

        delete("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()

            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "invalid id")
                return@delete
            }
            val ok = NewGradeClassRepository.delete(id)
            if (!ok) {
                call.respond(HttpStatusCode.NotFound, "Class not found")

            } else {
                call.respond(HttpStatusCode.NoContent)
            }

        }

        patch("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            println("id is $id")
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "invalid id")
                return@patch
            }

            val req = call.receive<PatchNewGradeClassRequest>()

            val updated = NewGradeClassRepository.patch(id, req)

            if (updated == null) {
                call.respond(HttpStatusCode.NotFound, "invalid id")
                return@patch

            } else {
                call.respond(HttpStatusCode.OK, updated)
            }
        }
    }
}