package com.example.academics




import com.example.academics.routes.gradeRoutes
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.gradeModule() {
    routing {
        route("/api") {
            route("/grades") {
                gradeRoutes()
            }
        }
    }
}