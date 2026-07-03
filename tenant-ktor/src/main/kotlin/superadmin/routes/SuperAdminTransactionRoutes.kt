package com.example.superadmin.routes


import com.example.superadmin.repo.SuperAdminTransactionRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.superAdminTransactionRoutes() {
    get {
        val tenantCode = call.request.queryParameters["tenantCode"]
        val status = call.request.queryParameters["status"]
        val paid = call.request.queryParameters["paid"]?.toBooleanStrictOrNull()

        val transactions = SuperAdminTransactionRepository.findAll(
            tenantCode = tenantCode,
            status = status,
            paid = paid
        )

        call.respond(HttpStatusCode.OK, transactions)
    }
}