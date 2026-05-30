package com.example.account

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.routing.route


fun Application.accountModule() {
    routing {
        route("/api") {

            route("/account") {
                accountRoutes()
            }


        }
    }
}




