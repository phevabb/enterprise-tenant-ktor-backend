package com.example.academics.routes



import com.example.academics.dtos.requests.CreateSubjectCategoryRequest
import com.example.academics.repos.SubjectCategoryRepository
import com.example.tenant.currentTenant
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.subjectCategoryRoutes() {

    get {

        val tenant = call.currentTenant()

        val data = SubjectCategoryRepository.findAll(
            tenantSchema = tenant.tenantSchema
        )

        call.respond(HttpStatusCode.OK, data)
    }

    post {

        val req = call.receive<CreateSubjectCategoryRequest>()

        val tenant = call.currentTenant()

        val result = SubjectCategoryRepository.create(
            tenantSchema = tenant.tenantSchema,
            req = req
        )

        call.respond(HttpStatusCode.Created, result)
    }

    put("{id}/subjects") {

        val id = call.parameters["id"]?.toIntOrNull()
        val req = call.receive<CreateSubjectCategoryRequest>()

        if (id == null) {
            call.respond(HttpStatusCode.BadRequest)
            return@put
        }

        val tenant = call.currentTenant()

        SubjectCategoryRepository.updateSubjects(
            tenantSchema = tenant.tenantSchema,
            id = id,
            subjectIds = req.subjectIds
        )

        call.respond(HttpStatusCode.OK)
    }

    delete("{id}") {

        val id = call.parameters["id"]?.toIntOrNull()

        if (id == null) {
            call.respond(HttpStatusCode.BadRequest)
            return@delete
        }

        val tenant = call.currentTenant()

        val ok = SubjectCategoryRepository.delete(
            tenantSchema = tenant.tenantSchema,
            id = id
        )

        if (!ok) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            call.respond(HttpStatusCode.NoContent)
        }
    }
}