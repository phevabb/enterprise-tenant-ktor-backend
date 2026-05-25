package com.example.academics.routes

import com.example.academics.repos.AcademicRecordRepository
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate

fun Route.academicRecordRoutes() {

    authenticate("auth-jwt") {

        /**
         * ✅ GET ALL academic records WITH inline subject scores
         * GET /api/academic-records
         */
        get {
            val records = AcademicRecordRepository.findAllWithScores()
            call.respond(HttpStatusCode.OK, records)
        }

        /**
         * ✅ GET single academic record WITH inline subject scores
         * GET /api/academic-records/{id}/detail
         */
        get("{id}/detail") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid id"))
                return@get
            }

            val record = AcademicRecordRepository.findByIdWithScores(id)
            if (record == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Academic record not found"))
            } else {
                call.respond(HttpStatusCode.OK, record)
            }
        }
    }
}


