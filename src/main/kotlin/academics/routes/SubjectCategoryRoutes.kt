package com.example.academics.routes



import com.example.academics.dtos.requests.CreateSubjectCategoryRequest
import com.example.academics.repos.SubjectCategoryRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.subjectCategoryRoutes() {

    // ✅ GET ALL
    get {
        val data = SubjectCategoryRepository.findAll()
        call.respond(HttpStatusCode.OK, data)
    }

    // ✅ CREATE
    post {
        val req = call.receive<CreateSubjectCategoryRequest>()

        val result = SubjectCategoryRepository.create(req)
        call.respond(HttpStatusCode.Created, result)
    }

    // ✅ UPDATE SUBJECTS
    put("{id}/subjects") {

        val id = call.parameters["id"]?.toIntOrNull()
        val req = call.receive<CreateSubjectCategoryRequest>()

        if (id == null) {
            call.respond(HttpStatusCode.BadRequest)
            return@put
        }

        SubjectCategoryRepository.updateSubjects(id, req.subjectIds)

        call.respond(HttpStatusCode.OK)
    }

    // ✅ DELETE
    delete("{id}") {

        val id = call.parameters["id"]?.toIntOrNull()

        if (id == null) {
            call.respond(HttpStatusCode.BadRequest)
            return@delete
        }

        val ok = SubjectCategoryRepository.delete(id)

        if (!ok) call.respond(HttpStatusCode.NotFound)
        else call.respond(HttpStatusCode.NoContent)
    }
}