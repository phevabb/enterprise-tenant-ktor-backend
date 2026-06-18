package com.example.staff

import com.example.staff.routes.staffRoutes
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.staffModule() {
    routing {
        route("/api") {

            route("/staff") {
                staffRoutes()
            }

        }
    }
}