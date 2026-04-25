package com.example

import com.example.account.accountModule
import com.example.student.studentModule
import io.ktor.server.application.*
import com.example.config.DatabaseFactory
import com.example.config.configureCors
import com.example.familyfees.familyModule
import com.example.fees.feeModule
import com.example.fees.routes.feeStructureRoutes

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSecurity()
    configureSerialization()
    accountModule()
    studentModule()
    configureCors()
    feeModule()
    familyModule()
    DatabaseFactory.init()
}
