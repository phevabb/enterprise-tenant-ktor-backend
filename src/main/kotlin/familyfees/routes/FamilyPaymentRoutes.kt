package com.example.familyfees.routes

import com.example.familyfees.dtos.requests.CreateFamilyPaymentRequest
import com.example.familyfees.repos.FamilyFeeRecordsRepository
import com.example.familyfees.repos.FamilyPaymentRepository
import com.example.notifications.SmsService
import io.ktor.server.routing.Route

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.familyPaymentRoutes() {

    // ✅ GET all family payments
    get {
        val allPayments = FamilyPaymentRepository.findAll()
        call.respond(HttpStatusCode.OK, allPayments)
    }

    // ✅ POST create family payment
    post {
        val req = call.receive<CreateFamilyPaymentRequest>()


        val result = FamilyPaymentRepository.createPaymentAndUpdateFfr(
            familyFeeRecordId = req.family_fee_record,
            amount = req.amount,
            paymentMethod = "cash"
        )

        println("results is $result")
        println("Server Instant now: " + java.time.Instant.now())
        println("Server millis now: " + System.currentTimeMillis())
//
        result.sms?.let { SmsService.sendAsync(it.phone, it.message) }

        call.respond(HttpStatusCode.Created, result.response)
    }

    delete("{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
        if (id == null) {
            call.respond(HttpStatusCode.BadRequest, "invalid ID")
            return@delete
        }
        val ok = FamilyPaymentRepository.delete(id)
        if(!ok){
            call.respond(HttpStatusCode.NotFound, "Family payment not found")
            return@delete
        }

        else  {
            call.respond(HttpStatusCode.NoContent)
        }

    }



}
