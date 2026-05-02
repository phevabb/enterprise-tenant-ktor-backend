package com.example

import com.example.account.accountModule
import com.example.commands.ImportStudentsFromCsv
import com.example.config.DatabaseFactory
import com.example.config.configureCors
import com.example.familyfees.familyModule
import com.example.fees.feeModule
import com.example.student.studentModule
import io.ktor.server.application.*



//
//fun main(args: Array<String>) {
//    io.ktor.server.netty.EngineMain.main(args)
//}
//






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



fun Application.module() {
    // ✅ Init DB early
    DatabaseFactory.init()

    configureSecurity()
    configureSerialization()
    configureCors()

    accountModule()
    studentModule()
    feeModule()
    familyModule()
}