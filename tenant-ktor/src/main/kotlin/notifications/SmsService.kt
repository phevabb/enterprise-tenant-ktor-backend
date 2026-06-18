package com.example.notifications

import com.example.fees.notifications.MnotifySmsRequest
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json

object SmsService {

    private val client: HttpClient by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        explicitNulls = false
                    }
                )
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val apiKey = "CelTN4i2JFPI2ZpknqYl0azod"
    private val endpoint = "https://api.mnotify.com/api/sms/quick"
    private val senderId = "KingOfGlory"

    fun sendAsync(phone: String, message: String) {
        if (apiKey.isBlank()) {
            println("SMS disabled: API key not configured")
            return
        }

        println("really $phone got hereeeeee")

        scope.launch {
            try {
                val payload = MnotifySmsRequest(
                    recipient = listOf(phone),
                    sender = senderId,
                    message = message
                )

                val response: HttpResponse = client.post("$endpoint?key=$apiKey") {
                    contentType(ContentType.Application.Json)
                    setBody(payload) // ✅ typed serializable object
                }

                val bodyText = response.bodyAsText()
                println("SMS status=${response.status} body=$bodyText")

            } catch (e: Exception) {
                println("new errorrrrrr is $e")
                e.printStackTrace()
            }
        }
    }

    fun close() {
        runCatching { client.close() }
        scope.cancel()
    }
}