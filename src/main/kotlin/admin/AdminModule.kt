package com.example.admin



import com.example.admin.routes.adminRoutes

import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.adminModule() {
    routing {
        route("/api") {

            route("/admin") {
                adminRoutes()
            }

        }
    }
}