package com.example.academics




import com.example.academics.routes.academicRecordRoutes
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.academicRecordModule() {
    routing {
        route("/api") {
            route("/academic-records") {
                academicRecordRoutes()
            }
        }
    }
}