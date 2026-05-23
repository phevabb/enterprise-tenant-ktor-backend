package com.example.academics.routes


import com.example.academics.dtos.requests.CreateSubjectRequest
import com.example.academics.repos.SubjectRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.subjectRoutes() {

    // GET /api/subjects
//    get {
//        val subjects = SubjectRepository.findAll()
//        call.respond(HttpStatusCode.OK, subjects)
//    }

    // POST /api/subjects
    post {
        val req = call.receive<CreateSubjectRequest>()
        val name = req.name.trim()

        if (name.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Name is required"))
            return@post
        }

        // Optional: prevent duplicate nicely instead of DB exception
        val existing = SubjectRepository.findByName(name)
        if (existing != null) {
            call.respond(HttpStatusCode.Conflict, mapOf("error" to "Subject already exists"))
            return@post
        }

        val created = SubjectRepository.create(name)
        call.respond(HttpStatusCode.Created, created)
    }

    // GET /api/subjects/{id}
    get("{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
        if (id == null) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid id"))
            return@get
        }

        val subject = SubjectRepository.findById(id)
        if (subject == null) {
            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Not found"))
        } else {
            call.respond(HttpStatusCode.OK, subject)
        }
    }

    // DELETE /api/subjects/{id}
    delete("{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
        if (id == null) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid id"))
            return@delete
        }

        val ok = SubjectRepository.delete(id)
        if (!ok) call.respond(HttpStatusCode.NotFound, mapOf("error" to "Not found"))
        else call.respond(HttpStatusCode.NoContent)
    }
}
