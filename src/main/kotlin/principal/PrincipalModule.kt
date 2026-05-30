package com.example.principal




import com.example.principal.routes.principalRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.principalModule() {

    routing {
        route("/api") {

            route("/principal") {
                principalRoutes()
            }

        }
    }

}