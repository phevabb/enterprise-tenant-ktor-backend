package com.example.academics




import com.example.academics.routes.academicRecordRoutes
import com.example.academics.routes.performanceChartRoutes
import com.example.academics.routes.studentAcademicRecordRoutes

import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.academicRecordModule() {
    routing {
        route("/api") {



            route("/academic-records") {
                academicRecordRoutes()
            }
            route("/student-academic-report-cards") {
                studentAcademicRecordRoutes()
            }

            route("/performance-charts") {
                performanceChartRoutes()
            }
        }
    }
}