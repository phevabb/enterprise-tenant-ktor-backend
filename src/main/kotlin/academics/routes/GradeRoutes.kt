package com.example.academics.routes


import com.example.academics.dtos.requests.CreateGradeRequest
import com.example.academics.dtos.requests.PatchGradeRequest
import com.example.academics.repos.GradeRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.gradeRoutes() {

    // ✅ GET ALL
    get {
        val grades = GradeRepository.findAll()
        call.respond(HttpStatusCode.OK, grades)
    }

    // ✅ CREATE
    post {
        val req = call.receive<CreateGradeRequest>()

        if (req.code.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Code is required"))
            return@post
        }

        if (req.minScore > req.maxScore) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid score range"))
            return@post
        }

        val existing = GradeRepository.findByCode(req.code)
        if (existing != null) {
            call.respond(HttpStatusCode.Conflict, mapOf("error" to "Grade code already exists"))
            return@post
        }

        val created = GradeRepository.create(req)
        call.respond(HttpStatusCode.Created, created)
    }

    // ✅ DELETE
    delete("{id}") {
        val id = call.parameters["id"]?.toIntOrNull()

        if (id == null) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid id"))
            return@delete
        }

        val ok = GradeRepository.delete(id)
        if (!ok) call.respond(HttpStatusCode.NotFound, mapOf("error" to "Not found"))
        else call.respond(HttpStatusCode.NoContent)
    }

    patch("{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
                    return@patch
                }

                val req = call.receive<PatchGradeRequest>()

                // Optional: quick check if request body is empty (all null)
                if (req.code == null && req.label == null && req.minScore == null && req.maxScore == null && req.order == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Nothing to update"))
                    return@patch
                }

                val updated = GradeRepository.patch(id, req)

                if (updated == null) {
                    // Could be: not found OR validation fail OR duplicate code
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Update failed (not found or invalid data / duplicate code)"))
                } else {
                    call.respond(HttpStatusCode.OK, updated)
                }
            }



}