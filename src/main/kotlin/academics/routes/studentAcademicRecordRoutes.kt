package com.example.academics.routes




import com.example.academics.repos.StudentAcademicRecordRepository
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.studentAcademicRecordRoutes() {

    authenticate("auth-jwt") {

        /**
         * ✅ GET /api/student-academic-records/student/{studentId}
         * Equivalent of Django by_student
         */
        get("student/{studentId}") {
            val studentId = call.parameters["studentId"]?.toIntOrNull()
            if (studentId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("detail" to "Invalid studentId"))
                return@get
            }

            val cards = StudentAcademicRecordRepository.findAllByStudentId(studentId)
            if (cards.isEmpty()) {
                call.respond(HttpStatusCode.NotFound, mapOf("detail" to "No academic records found."))
            } else {
                call.respond(HttpStatusCode.OK, cards)
            }
        }

        /**
         * ✅ GET /api/student-academic-records/user/{userId}
         * Equivalent of Django by_user (student__user_id)
         * Here userId is AccountTable.userId string
         */
        get("user/{userId}") {
            val userId = call.parameters["userId"]?.trim()
            if (userId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("detail" to "Invalid userId"))
                return@get
            }

            val cards = StudentAcademicRecordRepository.findAllByUserId(userId)
            if (cards.isEmpty()) {
                call.respond(HttpStatusCode.NotFound, mapOf("detail" to "No academic records found for this user."))
            } else {
                call.respond(HttpStatusCode.OK, cards)
            }
        }

        /**
         * ✅ GET /api/student-academic-records/record/{id}
         * Equivalent of Django get_record(pk)
         */
        get("record/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("detail" to "Invalid record id"))
                return@get
            }

            val card = StudentAcademicRecordRepository.findOneByRecordId(id)
            if (card == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("detail" to "Academic record not found"))
            } else {
                call.respond(HttpStatusCode.OK, card)
            }
        }
    }
}