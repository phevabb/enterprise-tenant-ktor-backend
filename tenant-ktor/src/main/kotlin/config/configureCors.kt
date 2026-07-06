package com.example.config

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS
import java.net.URI

fun Application.configureCors() {
    install(CORS) {

        /**
         * Production frontends:
         *
         * https://phenaschool.com
         * https://www.phenaschool.com
         * https://kingofgloryacademy.phenaschool.com
         * https://accraacademy.phenaschool.com
         */
        allowOrigins { origin ->
            val parsed = parseOrigin(origin) ?: return@allowOrigins false

            parsed.scheme == "https" &&
                    (
                            parsed.host == "phenaschool.com" ||
                                    parsed.host == "www.phenaschool.com" ||
                                    parsed.host.endsWith(".phenaschool.com")
                            )
        }

        /**
         * Local development frontends:
         *
         * http://localhost:3000
         * http://127.0.0.1:3000
         * http://phevabacademy.localhost:3000
         * http://kingofgloryacademy.localhost:3000
         * http://accraacademy.localhost:3000
         */
        allowOrigins { origin ->
            val parsed = parseOrigin(origin) ?: return@allowOrigins false

            parsed.scheme == "http" &&
                    parsed.port == 3000 &&
                    (
                            parsed.host == "localhost" ||
                                    parsed.host == "127.0.0.1" ||
                                    parsed.host.endsWith(".localhost")
                            )
        }

        /**
         * Optional Vercel preview domains:
         *
         * https://your-project-git-branch-user.vercel.app
         */
        allowOrigins { origin ->
            val parsed = parseOrigin(origin) ?: return@allowOrigins false

            parsed.scheme == "https" &&
                    parsed.host.endsWith(".vercel.app")
        }

        /**
         * Methods
         */
        anyHost()
        allowHeader("X-Tenant-Slug")
        allowHeader("X-Tenant-Code")
        allowHeader("X-School-Name")
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)

        /**
         * Standard headers
         */
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.Accept)
        allowHeader(HttpHeaders.Origin)

        /**
         * Tenant / school headers
         */
        allowHeader("X-Tenant-Slug")
        allowHeader("X-Tenant-Code")
        allowHeader("X-Tenant-Schema")
        allowHeader("X-School-Name")

        /**
         * Useful for Axios / AJAX
         */
        allowHeader("X-Requested-With")

        /**
         * Useful for PDF/file downloads
         */
        exposeHeader(HttpHeaders.ContentDisposition)

        /**
         * Needed for JSON/PDF requests with custom headers.
         */
        allowNonSimpleContentTypes = true

        /**
         * Keep true if you ever use cookies/credentials.
         * It is okay with explicit allowOrigins above.
         */
        allowCredentials = true

        maxAgeInSeconds = 3600

        /**
         * Do NOT use anyHost() together with allowCredentials = true.
         * Do NOT enable anyHost() in production.
         */
        // anyHost()
    }
}

private data class ParsedOrigin(
    val scheme: String,
    val host: String,
    val port: Int
)

private fun parseOrigin(origin: String): ParsedOrigin? {
    return try {
        val uri = URI(origin)

        val scheme = uri.scheme?.lowercase() ?: return null
        val host = uri.host?.lowercase() ?: return null

        val port = when {
            uri.port != -1 -> uri.port
            scheme == "https" -> 443
            scheme == "http" -> 80
            else -> -1
        }

        ParsedOrigin(
            scheme = scheme,
            host = host,
            port = port
        )
    } catch (e: Exception) {
        null
    }
}