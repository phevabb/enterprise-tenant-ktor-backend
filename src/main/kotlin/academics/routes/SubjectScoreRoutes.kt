package com.example.academics.routes

import com.example.academics.dtos.requests.CreateOrUpdateSubjectScoreRequest
import com.example.academics.dtos.requests.CreateSubjectScoreByStudentRequest
import com.example.academics.dtos.requests.PatchSubjectScoreRequest
import com.example.academics.repos.SubjectScoreRepository
import com.example.academics.services.SubjectScoreService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.subjectScoreRoutes() {

    authenticate("auth-jwt") {

        /**
         * ✅ GET ALL (expanded)
         * GET /api/subject-scores
         */
        get {
            val scores = SubjectScoreRepository.findAllExpanded()
            call.respond(HttpStatusCode.OK, scores)
        }

        /**
         * ✅ GET ONE (expanded)
         * GET /api/subject-scores/{id}
         */
        get("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid id"))
                return@get
            }

            val score = SubjectScoreRepository.findByIdExpanded(id)
            if (score == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Not found"))
            } else {
                call.respond(HttpStatusCode.OK, score)
            }
        }

        /**
         * ✅ GET BY ACADEMIC RECORD (expanded)
         * GET /api/subject-scores/record/{recordId}/expanded
         */
        get("record/{recordId}/expanded") {
            val recordId = call.parameters["recordId"]?.toIntOrNull()
            if (recordId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid recordId"))
                return@get
            }

            val scores = SubjectScoreRepository.findByAcademicRecordExpanded(recordId)
            call.respond(HttpStatusCode.OK, scores)
        }

        /**
         * ✅ CREATE / UPDATE (upsert)
         * POST /api/subject-scores
         *
         * Service should:
         * - compute totalScore/grade/interpretation
         * - recompute AcademicRecord.rawScoreTotal
         */
        post {
            try {
                val req = call.receive<CreateOrUpdateSubjectScoreRequest>()
                val result = SubjectScoreService.createOrUpdate(req)
                call.respond(HttpStatusCode.OK, result)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Invalid request")))
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Server error"))
            }
        }

        /**
         * ✅ PATCH (partial update by score id)
         * PATCH /api/subject-scores/{id}
         *
         * Service.patch should:
         * - update classScore/examScore
         * - recompute totalScore/grade/interpretation (Django-like save())
         * - recompute AcademicRecord.rawScoreTotal
         */
        patch("{id}") {
            try {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid id"))
                    return@patch
                }

                val req = call.receive<PatchSubjectScoreRequest>()

                // Optional guard: empty patch body
                if (req.classScore == null && req.examScore == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Nothing to update"))
                    return@patch
                }

                val updated = SubjectScoreService.patch(id, req)
                call.respond(HttpStatusCode.OK, updated)

            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to (e.message ?: "Not found")))
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Server error"))
            }
        }

        /**
         * ✅ DELETE (by score id)
         * DELETE /api/subject-scores/{id}
         *
         * Service.delete should:
         * - delete row
         * - recompute AcademicRecord.rawScoreTotal
         */
        delete("{id}") {
            try {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid id"))
                    return@delete
                }

                SubjectScoreService.delete(id)
                call.respond(HttpStatusCode.NoContent)

            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to (e.message ?: "Not found")))
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Server error"))
            }
        }

//        post("by-staff") {
//            try {
//                val req = call.receive<CreateSubjectScoreByStudentRequest>()
//                val result = SubjectScoreService.createOrUpdateByStudent(req)
//                call.respond(HttpStatusCode.OK, result)
//            } catch (e: IllegalArgumentException) {
//                call.respond(HttpStatusCode.BadRequest, mapOf("detail" to (e.message ?: "Invalid request")))
//            } catch (e: Exception) {
//                e.printStackTrace()
//                call.respond(HttpStatusCode.InternalServerError, mapOf("detail" to "Server error"))
//            }
//        }



        post("by-staff") {
            try {
                println("====== REQUEST START ======")

                // ✅ 1. Read raw JSON body
                val raw = call.receiveText()
                println("🔥 RAW REQUEST BODY: $raw")

                // ✅ 2. Deserialize manually
                val req = kotlinx.serialization.json.Json.decodeFromString(
                    CreateSubjectScoreByStudentRequest.serializer(),
                    raw
                )

                println("✅ Parsed request: $req")

                // ✅ 3. Process request
                val result = SubjectScoreService.createOrUpdateByStudent(req)

                println("✅ Success response: $result")
                println("====== REQUEST END ======")

                call.respond(HttpStatusCode.OK, result)

            } catch (e: IllegalArgumentException) {
                println("❌ Business error: ${e.message}")

                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("detail" to (e.message ?: "Invalid request"))
                )

            } catch (e: Exception) {
                println("====== ERROR ======")
                e.printStackTrace()

                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("detail" to "Server error")
                )
            }
        }

    }
}








