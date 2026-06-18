package com.example.academics.routes



import com.example.academics.dtos.requests.CreateSubjectRequest
import com.example.academics.repos.SubjectRepository
import com.example.tenant.currentTenant

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.auth.*


fun Route.subjectRoutes() {

    authenticate("auth-jwt") {

        // ✅ GET ALL SUBJECTS
        // ✅ GET ALL SUBJECTS
        get {
            val tenant = call.currentTenant()

            val subjects = SubjectRepository.findAll(
                tenantSchema = tenant.tenantSchema
            )

            call.respond(HttpStatusCode.OK, subjects)
        }

// ✅ GET SUBJECT BY ID
        get("{id}") {
            val tenant = call.currentTenant()

            val id = call.parameters["id"]?.toIntOrNull()

            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
                return@get
            }

            val subject = SubjectRepository.findById(
                tenantSchema = tenant.tenantSchema,
                id = id
            )

            if (subject == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Subject not found"))
            } else {
                call.respond(HttpStatusCode.OK, subject)
            }
        }

// ✅ CREATE SUBJECT
        post {
            val tenant = call.currentTenant()

            val req = call.receive<CreateSubjectRequest>()

            if (req.name.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Name is required"))
                return@post
            }

            val created = SubjectRepository.create(
                tenantSchema = tenant.tenantSchema,
                req = req
            )

            call.respond(HttpStatusCode.Created, created)
        }

// ✅ UPDATE SUBJECT
        patch("{id}") {
            val tenant = call.currentTenant()

            val id = call.parameters["id"]?.toIntOrNull()

            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
                return@patch
            }

            val req = call.receive<CreateSubjectRequest>()

            val updated = SubjectRepository.update(
                tenantSchema = tenant.tenantSchema,
                id = id,
                req = req
            )

            if (updated == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Subject not found"))
            } else {
                call.respond(HttpStatusCode.OK, updated)
            }
        }

// ✅ DELETE SUBJECT
        delete("{id}") {
            val tenant = call.currentTenant()

            val id = call.parameters["id"]?.toIntOrNull()

            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
                return@delete
            }

            val deleted = SubjectRepository.delete(
                tenantSchema = tenant.tenantSchema,
                id = id
            )

            if (!deleted) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Subject not found"))
            } else {
                call.respond(HttpStatusCode.NoContent)
            }
        }

// ✅ GET SUBJECTS BY CATEGORY
        get("/category/{categoryId}") {
            val tenant = call.currentTenant()

            val categoryId = call.parameters["categoryId"]?.toIntOrNull()

            if (categoryId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid categoryId"))
                return@get
            }

            val subjects = SubjectRepository.findByCategory(
                tenantSchema = tenant.tenantSchema,
                categoryId = categoryId
            )

            call.respond(HttpStatusCode.OK, subjects)
        }
    }
}