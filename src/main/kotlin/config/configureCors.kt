package com.example.config


import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.http.*

fun Application.configureCors() {
    install(CORS) {
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)

        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)

        // ✅ Your frontend dev server origin:
        allowHost("localhost:5173", schemes = listOf("http"))  // Vite
        allowHost("127.0.0.1:5173", schemes = listOf("http"))
        // If using Vue CLI:
        // allowHost("localhost:8081", schemes = listOf("http"))

        allowCredentials = true
        anyHost() // ⚠️ DEV ONLY. Remove in production.
    }
}