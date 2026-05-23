package com.example

import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*



import kotlinx.serialization.json.Json

fun Application.configureSerialization() {

    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true   // ✅ ignore extra JSON fields
                isLenient = true          // ✅ allow relaxed JSON
                prettyPrint = true        // ✅ optional (for responses)
                encodeDefaults = true     // ✅ include default values
            }
        )
    }
}