package com.example.student

import com.example.student.routes.academicYearRoutes
import com.example.student.routes.gradeClassRoutes
import com.example.student.routes.studentRoutes

import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.studentModule() {
    routing {
        route("/api") {
            route("/student") {
                studentRoutes()
            }
            route("/grade-class") {
                gradeClassRoutes()
            }

            route("/year"){
                academicYearRoutes()
            }
        }
    }
}
