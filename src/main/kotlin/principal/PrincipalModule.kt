package com.example.principal




import com.example.fees.routes.feeStructureRoutes
import com.example.fees.routes.paymentRoutes
import com.example.fees.routes.receiptRoutes
import com.example.fees.routes.studentFeeRecordRoutes
import com.example.principal.routes.principalRoutes
import com.example.principal.service.PrincipalService
import io.ktor.server.application.Application
import io.ktor.server.routing.route
import io.ktor.server.routing.routing


import com.example.principal.dtos.requests.CreatePrincipalRequest

import com.example.tenant.currentTenant
import com.example.tenant.requireTenantFeature
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.principalModule() {
    routing {
        authenticate("auth-jwt") {

            route("/api") {
                route("/principal") {
                    principalRoutes()
                }
            }

        }
    }
}

