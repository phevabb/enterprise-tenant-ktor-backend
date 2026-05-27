package com.example.academics.routes

import com.example.academics.dtos.requests.NewCreateAcademicRecordRequest
import com.example.academics.repos.AcademicRecordRepository
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import com.example.academics.dtos.requests.PatchAcademicRecordRemarksRequest
import kotlin.text.get

fun Route.academicRecordRoutes() {

    authenticate("auth-jwt") {

        // ✅ GET ALL
        get {
            val records = AcademicRecordRepository.findAllWithScores()
            call.respond(HttpStatusCode.OK, records)
        }

        // ✅ GET ONE
        get("{id}/detail") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid id"))
                return@get
            }

            val record = AcademicRecordRepository.findByIdWithScores(id)
            if (record == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Not found"))
            } else {
                call.respond(HttpStatusCode.OK, record)
            }
        }

        // ✅ PATCH (update remarks)
        patch("{id}/remarks") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@patch call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid id"))

            val payload = call.receive<PatchAcademicRecordRemarksRequest>()

            val updated = AcademicRecordRepository.updateRemarks(id, payload)

            call.respond(HttpStatusCode.OK, updated)
        }

        get("class/{classId}") {
            val classId = call.parameters["classId"]?.toIntOrNull()

            if (classId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid classId"))
                return@get
            }

            val records = AcademicRecordRepository.findByClass(classId)

            call.respond(HttpStatusCode.OK, records)
        }

        get("class/{classId}/current") {
            val classId = call.parameters["classId"]?.toIntOrNull()
            if (classId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid classId"))
                return@get
            }

            val records = AcademicRecordRepository.findByClassCurrent(classId)
            call.respond(HttpStatusCode.OK, records)
        }


        delete("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid id"))

            val ok = AcademicRecordRepository.deleteById(id)
            if (!ok) call.respond(HttpStatusCode.NotFound, mapOf("error" to "Not found"))
            else call.respond(HttpStatusCode.NoContent)
        }






    }
}







