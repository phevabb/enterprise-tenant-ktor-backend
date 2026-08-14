package com.example.admin



//import account.schoolLogoRoutes
import com.example.account.accountProfilePictureRoutes
import com.example.admin.routes.adminRoutes

import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.adminModule() {
    routing {
        route("/api") {

            route("/admin") {
                adminRoutes()
            }

            route("/profile-picture") {
                accountProfilePictureRoutes()
            }

//            route("/school-logo") {
//
//                schoolLogoRoutes()
//            }

        }
    }
}