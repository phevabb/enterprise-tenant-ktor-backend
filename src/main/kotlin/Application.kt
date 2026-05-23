package com.example

import com.auth0.jwt.JWT
import com.example.academics.academicRecordModule
import com.example.academics.categoryModule
import com.example.academics.gradeModule
import com.example.academics.subjectCategoryModule
import com.example.academics.subjectModule
import com.example.academics.subjectScoreModule
import com.example.account.accountModule
import com.example.admin.adminModule
import com.example.commands.ImportStudentsFromCsv
import com.example.config.DatabaseFactory
import com.example.config.configureCors
import com.example.familyfees.familyModule
import com.example.fees.feeModule
import com.example.staff.staffModule
import com.example.student.studentModule
import io.ktor.server.application.*


import com.example.auth.JwtConfig
import com.example.auth.authModule
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

import com.auth0.jwt.algorithms.Algorithm





fun main(args: Array<String>) {

    when (args.firstOrNull()) {

        // ✅ Normal Ktor server
//        "server", null -> {
//            io.ktor.server.netty.EngineMain.main(args)
//        }  later

        "server", null -> io.ktor.server.netty.EngineMain.main(emptyArray())

        // ✅ Django-style management command
        "import-students" -> {
            ImportStudentsFromCsv.run()
        }

        else -> {
            println(
                """
                Unknown command: ${args.first()}
                
                Usage:
                  server                 Run Ktor backend
                  import-students        Import students from CSV
                """.trimIndent()
            )
        }
    }
}


fun Application.configureAuth() {

    val secret = "super-secret"   // ✅ MUST match your JwtConfig
    val audience = "ktor-users"   // ✅ MUST match token
    val issuer = "ktor-api"       // ✅ MUST match token

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
    // ✅ Init DB early
    DatabaseFactory.init()
    configureAuth()

    configureSecurity()
    configureSerialization()
    configureCors()


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
}