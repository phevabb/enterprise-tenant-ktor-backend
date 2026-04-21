package com.example.account
//this file adds account-related routes to the main app
import io.ktor.server.application.*
import io.ktor.server.routing.*


fun Application.accountModule() {
    routing {
        route("/account") {
            accountRoutes()
        }
    }
}