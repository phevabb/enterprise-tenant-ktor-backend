package com.example

import com.example.account.accountModule
import com.example.student.studentModule
import io.ktor.server.application.*
import com.example.config.DatabaseFactory
import com.example.config.configureCors

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSecurity()
    configureSerialization()
    accountModule()
    studentModule()
    configureCors()
    DatabaseFactory.init()
}
