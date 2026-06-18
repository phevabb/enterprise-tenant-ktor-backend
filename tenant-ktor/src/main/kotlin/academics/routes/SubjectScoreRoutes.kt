package com.example.academics.routes

import com.example.academics.dtos.requests.CreateOrUpdateSubjectScoreRequest
import com.example.academics.dtos.requests.CreateSubjectScoreByStudentRequest
import com.example.academics.dtos.requests.PatchSubjectScoreRequest
import com.example.academics.repos.SubjectRepoLite
import com.example.academics.repos.SubjectScoreRepository
import com.example.academics.services.SubjectScoreService
import com.example.tenant.currentTenant
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.subjectScoreRoutes() {

    authenticate("auth-jwt") {


        get {
            val tenant = call.currentTenant()

            val scores = SubjectScoreRepository.findAllExpanded(
                tenantSchema = tenant.tenantSchema
            )

            call.respond(HttpStatusCode.OK, scores)
        }
        get("{id}") {
            val tenant = call.currentTenant()

            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid id"))
                return@get
            }

            val score = SubjectScoreRepository.findByIdExpanded(
                tenantSchema = tenant.tenantSchema,
                id = id
            )

            if (score == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Not found"))
            } else {
                call.respond(HttpStatusCode.OK, score)
            }
        }


        get("record/{recordId}/expanded") {
            val tenant = call.currentTenant()

            val recordId = call.parameters["recordId"]?.toIntOrNull()
            if (recordId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid recordId"))
                return@get
            }

            val scores = SubjectScoreRepository.findByAcademicRecordExpanded(
                tenantSchema = tenant.tenantSchema,
                recordId = recordId
            )

            call.respond(HttpStatusCode.OK, scores)
        }



        post {
            val tenant = call.currentTenant()

            try {
                val req = call.receive<CreateOrUpdateSubjectScoreRequest>()

                val result = SubjectScoreService.createOrUpdate(
                    tenantSchema = tenant.tenantSchema,
                    req = req
                )

                call.respond(HttpStatusCode.OK, result)

            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Invalid request")))
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Server error"))
            }
        }



        patch("{id}") {
            val tenant = call.currentTenant()

            try {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid id"))
                    return@patch
                }

                val req = call.receive<PatchSubjectScoreRequest>()

                if (req.classScore == null && req.examScore == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Nothing to update"))
                    return@patch
                }

                val updated = SubjectScoreService.patch(
                    tenantSchema = tenant.tenantSchema,
                    scoreId = id,
                    req = req
                )

                call.respond(HttpStatusCode.OK, updated)

            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to (e.message ?: "Not found")))
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Server error"))
            }
        }



        delete("{id}") {
            val tenant = call.currentTenant()

            try {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid id"))
                    return@delete
                }

                SubjectScoreService.delete(
                    tenantSchema = tenant.tenantSchema,
                    scoreId = id
                )

                call.respond(HttpStatusCode.NoContent)

            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to (e.message ?: "Not found")))
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Server error"))
            }
        }


        post("by-staff") {
            val tenant = call.currentTenant()

            try {
                // ✅ 1. Read raw JSON body
                val raw = call.receiveText()

                // ✅ 2. Deserialize manually
                val req = kotlinx.serialization.json.Json.decodeFromString(
                    CreateSubjectScoreByStudentRequest.serializer(),
                    raw
                )

                // ✅ 3. Process request
                val result = SubjectScoreService.createOrUpdateByStudent(
                    tenantSchema = tenant.tenantSchema,
                    req = req
                )

                call.respond(HttpStatusCode.OK, result)

            } catch (e: IllegalArgumentException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("detail" to (e.message ?: "Invalid request"))
                )
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("detail" to "Server error")
                )
            }
        }



        get("context") {
            val tenant = call.currentTenant()

            val classId = call.request.queryParameters["classId"]?.toIntOrNull()
            val termId = call.request.queryParameters["termId"]?.toIntOrNull()
            val yearId = call.request.queryParameters["yearId"]?.toIntOrNull()

            if (classId == null || termId == null || yearId == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "classId, termId and yearId are required")
                )
                return@get
            }

            val subjectParam = call.request.queryParameters["subject"]?.trim()

            // subject can be:
            // - null (fetch all subjects)
            // - "6" (id)
            // - "English Language" (name)
            val subjectId = subjectParam?.let {
                val tenant = call.currentTenant()
                SubjectRepoLite.findIdByIdOrName(tenantSchema = tenant.tenantSchema, it)
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid subject"))
            }

            val scores = SubjectScoreRepository.findByContext(
                tenantSchema = tenant.tenantSchema,
                classLevelId = classId,
                termId = termId,
                academicYearId = yearId,
                subjectId = subjectId
            )

            call.respond(HttpStatusCode.OK, scores)
        }


    }
}








