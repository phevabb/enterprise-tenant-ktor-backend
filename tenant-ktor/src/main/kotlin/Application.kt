package com.example



import attendance.route.configureAttendanceRoutes
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.application.install
import io.ktor.server.auth.jwt.jwt



import chat.module.configureRouting
import com.example.academics.academicRecordModule
import com.example.academics.categoryModule
import com.example.academics.gradeModule
import com.example.academics.subjectCategoryModule
import com.example.academics.subjectModule
import com.example.academics.subjectScoreModule
import com.example.account.accountModule
import com.example.admin.adminModule
import com.example.auth.authModule
import com.example.billing.billingModule

import com.example.config.DatabaseFactory
import com.example.config.configureCors
import com.example.familyfees.familyModule
import com.example.fees.feeModule
import com.example.principal.principalModule
import com.example.staff.staffModule
import com.example.student.studentModule
import com.example.superadmin.superAdminModule
import com.example.tenant.TenantPlugin
import com.example.tenant.TenantResolver
import com.example.tenant.module.tenantModule
import com.example.tenant.services.TenantRegistryService
import io.ktor.server.application.Application
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import com.example.tenant.tenantAdminModule
import complaints.complaintModule
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.server.auth.parseAuthorizationHeader
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

fun main(args: Array<String>) {
    when (args.firstOrNull()) {
        "server", null -> {
            io.ktor.server.netty.EngineMain.main(emptyArray())
        }

        "import-students" -> {
            DatabaseFactory.init()

            val tenantCode = args.getOrNull(1)
                ?: error("Usage: import-students <tenantCode>")

            val tenantSchema = TenantRegistryService.findTenantSchemaByTenantCode(tenantCode)
                ?: error("No tenant found for tenantCode='$tenantCode'")

            println("===== IMPORT STUDENTS COMMAND =====")
            println("tenantCode = $tenantCode")
            println("tenantSchema = $tenantSchema")


        }

        else -> {
            println(
                """
                Unknown command: ${args.firstOrNull()}
                
                Usage:
                  server
                  import-students <tenantCode>
                """.trimIndent()
            )
        }
    }
}



fun Application.configureAuth() {

    val secret =
        "super-secret"

    val audience =
        "ktor-users"

    val issuer =
        "ktor-api"

    install(Authentication) {

        jwt("auth-jwt") {

            /*
             * REST requests:
             * Authorization: Bearer TOKEN
             *
             * Browser WebSocket requests:
             * /chat/ws?token=TOKEN
             */
            authHeader { call ->

                /*
                 * Normal REST authentication:
                 *
                 * Authorization: Bearer TOKEN
                 */
                val authorizationHeader =
                    call.request.parseAuthorizationHeader()

                if (authorizationHeader != null) {
                    return@authHeader authorizationHeader
                }

                /*
                 * Browser WebSocket authentication.
                 *
                 * Existing student frontend:
                 * ?token=TOKEN
                 *
                 * Teacher frontend:
                 * ?access_token=TOKEN
                 */
                val queryToken =
                    (
                            call.request
                                .queryParameters["access_token"]
                                ?.trim()
                                ?.takeIf { token ->
                                    token.isNotBlank()
                                }
                                ?: call.request
                                    .queryParameters["token"]
                                    ?.trim()
                                    ?.takeIf { token ->
                                        token.isNotBlank()
                                    }
                            )

                if (queryToken == null) {
                    null
                } else {
                    HttpAuthHeader.Single(
                        authScheme = "Bearer",
                        blob = queryToken
                    )
                }
            }

            verifier(
                JWT
                    .require(
                        Algorithm.HMAC256(
                            secret
                        )
                    )
                    .withAudience(
                        audience
                    )
                    .withIssuer(
                        issuer
                    )
                    .build()
            )

            validate { credential ->

                val userId =
                    credential.payload
                        .getClaim("userId")
                        .asInt()

                if (
                    userId != null &&
                    userId > 0
                ) {
                    JWTPrincipal(
                        credential.payload
                    )
                } else {
                    null
                }
            }
        }
    }
}



fun Application.configureWebSockets() {
    install(WebSockets) {
        pingPeriod = 20.seconds
        timeout = 20.seconds
        maxFrameSize = 64 * 1024
        masking = false

        contentConverter =
            KotlinxWebsocketSerializationConverter(
                Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                }
            )
    }
}


fun Application.module() {
    DatabaseFactory.init()

    configureAuth()
    configureWebSockets()
    configureSecurity()
    configureSerialization()
    configureCors()
    tenantModule()
    superAdminModule()
    configureAttendanceRoutes()


    install(TenantPlugin) {
        resolver = TenantResolver()
    }

    tenantAdminModule()
    configureRouting()

    accountModule()
    studentModule()
    feeModule()
    familyModule()
    staffModule()
    adminModule()
    billingModule()


    authModule()
    subjectModule()
    academicRecordModule()
    gradeModule()
    subjectScoreModule()
    categoryModule()
    subjectCategoryModule()
    principalModule()
    complaintModule()


}

























