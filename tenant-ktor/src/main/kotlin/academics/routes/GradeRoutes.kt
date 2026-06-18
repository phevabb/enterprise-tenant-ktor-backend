package com.example.academics.routes


import com.example.academics.dtos.requests.CreateGradeRequest
import com.example.academics.dtos.requests.PatchGradeRequest
import com.example.academics.repos.GradeRepository
import com.example.tenant.currentTenant
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.gradeRoutes() {

    get {
        val tenant = call.currentTenant()

        val grades = GradeRepository.findAll(
            tenantSchema = tenant.tenantSchema
        )

        call.respond(HttpStatusCode.OK, grades)
    }

    post {
        val tenant = call.currentTenant()

        val req = call.receive<CreateGradeRequest>()

        if (req.code.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Code is required"))
            return@post
        }

        if (req.minScore > req.maxScore) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid score range"))
            return@post
        }

        val existing = GradeRepository.findByCode(
            tenantSchema = tenant.tenantSchema,
            code = req.code
        )

        if (existing != null) {
            call.respond(
                HttpStatusCode.Conflict,
                mapOf("error" to "Grade code already exists")
            )
            return@post
        }

        val created = GradeRepository.create(
            tenantSchema = tenant.tenantSchema,
            req = req
        )

        call.respond(HttpStatusCode.Created, created)
    }

    delete("{id}") {

        val tenant = call.currentTenant()

        val id = call.parameters["id"]?.toIntOrNull()

        if (id == null) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid id"))
            return@delete
        }

        val ok = GradeRepository.delete(
            tenantSchema = tenant.tenantSchema,
            id = id
        )

        if (!ok) {
            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Not found"))
        } else {
            call.respond(HttpStatusCode.NoContent)
        }
    }

    patch("{id}") {

        val tenant = call.currentTenant()

        val id = call.parameters["id"]?.toIntOrNull()

        if (id == null) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
            return@patch
        }

        val req = call.receive<PatchGradeRequest>()

        if (
            req.code == null &&
            req.label == null &&
            req.minScore == null &&
            req.maxScore == null &&
            req.order == null
        ) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Nothing to update")
            )
            return@patch
        }

        val updated = GradeRepository.patch(
            tenantSchema = tenant.tenantSchema,
            id = id,
            req = req
        )

        if (updated == null) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Update failed (not found or invalid data / duplicate code)")
            )
        } else {
            call.respond(HttpStatusCode.OK, updated)
        }
    }
}