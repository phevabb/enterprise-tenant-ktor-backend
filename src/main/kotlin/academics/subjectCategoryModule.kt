package com.example.academics




import com.example.academics.routes.subjectCategoryRoutes

import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.subjectCategoryModule() {
    routing {
        route("/api") {
            route("/subject-categories") {
                subjectCategoryRoutes()
            }
        }
    }
}