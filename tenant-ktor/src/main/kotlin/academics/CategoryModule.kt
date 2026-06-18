package com.example.academics


import com.example.academics.routes.categoryRoutes

import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.categoryModule() {

    routing {
        route("/api") {

            route("/categories") {
                categoryRoutes()
            }

        }
    }
}