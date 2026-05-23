package com.example.academics


import com.example.academics.routes.subjectScoreRoutes

import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.subjectScoreModule() {
    routing {
        route("/api") {
            route("/subject-scores") {
                subjectScoreRoutes()
            }
        }
    }
}