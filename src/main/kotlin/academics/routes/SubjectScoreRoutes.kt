package com.example.academics.routes


import com.example.academics.dtos.requests.CreateSubjectScoreRequest
import com.example.academics.repos.SubjectScoreRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.subjectScoreRoutes() {

    // ✅ CREATE / UPDATE
    post {
        val req = call.receive<CreateSubjectScoreRequest>()

        val result = SubjectScoreRepository.createOrUpdate(req)

        call.respond(HttpStatusCode.OK, result)
    }

    // ✅ GET BY RECORD
    get("/record/{recordId}") {

        val id = call.parameters["recordId"]?.toIntOrNull()

        if (id == null) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid id"))
            return@get
        }

        val scores = SubjectScoreRepository.findByAcademicRecord(id)

        call.respond(HttpStatusCode.OK, scores)
    }
}