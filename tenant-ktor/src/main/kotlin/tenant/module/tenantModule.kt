package com.example.tenant.module





import com.example.tenant.routes.tenantRoutes
import io.ktor.server.application.*
import io.ktor.server.routing.*



fun Application.tenantModule() {

    routing {
        route("/api") {

            route("/tenant") {
                tenantRoutes()
            }





        }
    }
}