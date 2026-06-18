package com.example.auth




import com.example.staff.routes.staffRoutes
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.authModule() {
    routing {
        route("/api") {

            route("/auth") {
                authRoutes()

            }

        }
    }
}
