package com.example

import com.example.account.accountModule
import com.example.config.DatabaseFactory
import com.example.config.configureCors
import com.example.familyfees.familyModule
import com.example.fees.feeModule
import com.example.student.studentModule
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
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