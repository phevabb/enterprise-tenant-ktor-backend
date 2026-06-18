package com.example.account

import io.ktor.server.routing.*
import io.ktor.server.response.*

fun Route.accountRoutes() {

    get {
        call.respondText("all accounts")
    }

    post {
        call.respondText("insert account")
    }

}