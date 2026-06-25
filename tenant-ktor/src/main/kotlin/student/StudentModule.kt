package com.example.student

import com.example.student.routes.academicYearRoutes
import com.example.student.routes.classPromotionRoutes
import com.example.student.routes.gradeClassRoutes
import com.example.student.routes.studentImportRoutes
import com.example.student.routes.studentRoutes
import com.example.student.routes.termRoutes

import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.studentModule() {
    routing {
        route("/api") {
            route("/student") {
                studentRoutes()
            }

            route("/student-import") {
                studentImportRoutes()
            }


            route("/grade-class") {
                gradeClassRoutes()
            }

            route("/year"){
                academicYearRoutes()
            }

            route("/term"){
                termRoutes()
            }

            route("/promotion"){
                classPromotionRoutes()
            }
        }
    }
}
