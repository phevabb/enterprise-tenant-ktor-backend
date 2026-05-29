package com.example.fees

import com.example.fees.routes.feeStructureRoutes
import com.example.fees.routes.paymentRoutes
import com.example.fees.routes.receiptRoutes
import com.example.fees.routes.studentFeeRecordRoutes
import com.example.student.routes.academicYearRoutes
import com.example.student.routes.gradeClassRoutes
import com.example.student.routes.studentRoutes
import com.example.student.routes.termRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.feeModule() {

    routing {
        route("/api") {
            route("/fee-structure") {
                feeStructureRoutes()
            }

            route("/fee-records") {
                studentFeeRecordRoutes()
            }

            route("/payment") {
                paymentRoutes()
            }


                route("/receipts") {
                    receiptRoutes()
                }

        }
    }

}