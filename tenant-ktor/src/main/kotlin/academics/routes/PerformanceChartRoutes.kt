package com.example.academics.routes


import com.example.academics.repos.PerformanceChartRepository
import com.example.student.repos.StudentLiteRepo
import com.example.student.repos.TermRepository
import com.example.tenant.currentTenant
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.performanceChartRoutes() {

    authenticate("auth-jwt") {


        get {
            val tenant = call.currentTenant()

            val accountId = call.request.queryParameters["student"]?.toIntOrNull()
            val termId = call.request.queryParameters["term"]?.toIntOrNull()
            val yearId = call.request.queryParameters["year"]?.toIntOrNull()

            if (accountId == null || termId == null || yearId == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("detail" to "student (accountId), term and year are required")
                )
                return@get
            }

            // ✅ Resolve StudentsTable.id from AccountTable.id
            val studentProfileId = StudentLiteRepo.getStudentProfileIdByAccountId(tenantSchema = tenant.tenantSchema, accountId)
            if (studentProfileId == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("detail" to "No student profile found for this user"))
                return@get
            }

            val chart = PerformanceChartRepository.getStudentChart(
                tenantSchema = tenant.tenantSchema,
                studentId = studentProfileId,
                termId = termId,
                yearId = yearId
            )

            if (chart == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("detail" to "No academic record found for this student/term/year")
                )
            } else {
                call.respond(HttpStatusCode.OK, chart)
            }
        }
    }
}





