package com.example.config

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS

fun Application.configureCors() {
    install(CORS) {

        // ✅ Allow your production frontend (VERY IMPORTANT)
        allowHost("enterprise-tenant-vue-frontend.vercel.app", schemes = listOf("https"))
        allowHost("www.kogschool.com", schemes = listOf("https"))
        allowHost("kogschool.com", schemes = listOf("https"))

        // ✅ Local development
        allowHost("localhost:3000", schemes = listOf("http"))
        allowHost("127.0.0.1:3000", schemes = listOf("http"))

        // ✅ Methods
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)

        // ✅ Headers
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.Accept)
        allowHeader("X-Tenant-Slug")
        allowHeader("X-Tenant-Code")

        allowCredentials = true


         anyHost()
    }
}