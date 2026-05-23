package com.example.academics



import com.example.academics.routes.subjectRoutes

import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.subjectModule() {
    routing {
        route("/api") {
            route("/subjects") {
                subjectRoutes()
            }
        }
    }
}