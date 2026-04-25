package com.example.familyfees

import com.example.familyfees.routes.familyFeeRecordsRoutes
import com.example.familyfees.routes.familyRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.familyModule() {
    routing {
        route("/api") {
            route("/family") {
                familyRoutes()
            }
            route("/family-fee-record") {
                familyFeeRecordsRoutes()
            }


        }

    }
}