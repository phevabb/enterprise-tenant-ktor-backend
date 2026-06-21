package com.example



import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.academics.academicRecordModule
import com.example.academics.categoryModule
import com.example.academics.gradeModule

import com.example.academics.subjectCategoryModule
import com.example.academics.subjectModule
import com.example.academics.subjectScoreModule
import com.example.account.accountModule
import com.example.admin.adminModule
import com.example.auth.authModule
import com.example.commands.ImportStudentsFromCsv
import com.example.config.DatabaseFactory
import com.example.config.configureCors
import com.example.familyfees.familyModule
import com.example.fees.feeModule
import com.example.principal.principalModule
import com.example.staff.staffModule
import com.example.student.studentModule
import com.example.superadmin.superAdminModule
import com.example.tenant.routes.tenantRoutes
import com.example.tenant.TenantPlugin
import com.example.tenant.TenantResolver
import com.example.tenant.module.tenantModule
import com.example.tenant.services.TenantRegistryService

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import com.example.tenant.tenantAdminModule
import io.ktor.server.auth.jwt.jwt

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

            ImportStudentsFromCsv.run(tenantSchema)
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
    val secret = "super-secret"
    val audience = "ktor-users"
    val issuer = "ktor-api"

    install(Authentication) {
        jwt("auth-jwt") {
            verifier(
                JWT
                    .require(Algorithm.HMAC256(secret))
                    .withAudience(audience)
                    .withIssuer(issuer)
                    .build()
            )

            validate { credential ->
                val userId = credential.payload.getClaim("userId").asInt()
                if (userId != null) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
}

fun Application.module() {
    DatabaseFactory.init()

    configureAuth()
    configureSecurity()
    configureSerialization()
    configureCors()
    tenantModule()
    superAdminModule()


    install(TenantPlugin) {
        resolver = TenantResolver()
    }

    tenantAdminModule()

    accountModule()
    studentModule()
    feeModule()
    familyModule()
    staffModule()
    adminModule()

    authModule()
    subjectModule()
    academicRecordModule()
    gradeModule()
    subjectScoreModule()
    categoryModule()
    subjectCategoryModule()
    principalModule()


}






















