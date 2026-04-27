package com.example.fees.routes

import com.example.fees.dtos.requests.CreatePaymentRequest
import com.example.notifications.SmsService
import com.example.fees.repos.PaymentRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.paymentRoutes() {

    get{
        val allPayments = PaymentRepository.findAll()

        call.respond(HttpStatusCode.OK, allPayments)
    }

//    post {
//
//        val req = call.receive<CreatePaymentRequest>()
//        val created = PaymentRepository.createPaymentAndUpdateSfr(req.student_fee_record_id, req.amount)
//        println("the error $created")
//        call.respond(HttpStatusCode.Created, created)
//    }


    post {
        val req = call.receive<CreatePaymentRequest>()

        val result = PaymentRepository.createPaymentAndUpdateSfr(
            studentFeeRecordId = req.student_fee_record_id,
            amount = req.amount,
            paymentMethod = "cash"
        )

        println("results.sms is ${result.sms}")
        // ✅ send only after transaction succeeded
//         result.sms?.let { SmsService.sendAsync(it.phone, it.message) }

        call.respond(HttpStatusCode.Created, result)
    }

    delete ("{id}"){
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@delete call.respond(HttpStatusCode.BadRequest, "invalid id")


        val ok = PaymentRepository.delete(id)
        if (!ok) {
            call.respond(HttpStatusCode.NotFound, "Student fee record not found")
        } else {
            call.respond(HttpStatusCode.NoContent)
        }
    }
}