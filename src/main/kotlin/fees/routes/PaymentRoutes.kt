package com.example.fees.routes

import com.example.fees.dtos.requests.CreatePaymentRequest
import com.example.fees.repos.PaymentRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.paymentRoutes() {

    get{
        val allPayments = PaymentRepository.findAll()
        println("err is $allPayments")
        call.respond(HttpStatusCode.OK, allPayments)
    }

    post {

        val req = call.receive<CreatePaymentRequest>()
        val created = PaymentRepository.create(req.student_fee_record_id, req.amount)
        println("the error $created")
        call.respond(HttpStatusCode.Created, created)
    }
}