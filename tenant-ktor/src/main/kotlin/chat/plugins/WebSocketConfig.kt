package chat.plugins



import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

fun Application.configureWebSockets() {
    install(WebSockets) {
        pingPeriod = 20.seconds
        timeout = 30.seconds
        maxFrameSize = 64 * 1024
        masking = false

        contentConverter =
            KotlinxWebsocketSerializationConverter(
                Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                    explicitNulls = false
                }
            )
    }
}