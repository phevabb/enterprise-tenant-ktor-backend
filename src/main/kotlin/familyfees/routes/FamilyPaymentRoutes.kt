package com.example.familyfees.routes

import com.example.familyfees.dtos.requests.CreateFamilyPaymentRequest

import com.example.familyfees.repos.FamilyPaymentRepository
import com.example.notifications.SmsService
import com.example.tenant.currentTenant

import io.ktor.server.routing.Route

import io.ktor.http.*

import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.familyPaymentRoutes() {

    // ✅ GET all family payments
    get {
        val tenant = call.currentTenant()
        val tenantSchema = tenant.tenantSchema

        val allPayments = FamilyPaymentRepository.findAll(tenantSchema)

        call.respond(HttpStatusCode.OK, allPayments)
    }

    post {
        val tenant = call.currentTenant()
        val tenantSchema = tenant.tenantSchema

        val req = call.receive<CreateFamilyPaymentRequest>()

        val result = FamilyPaymentRepository.createPaymentAndUpdateFfr(
            tenantSchema = tenantSchema,
            familyFeeRecordId = req.family_fee_record,
            amount = req.amount,
            paymentMethod = "cash"
        )

//    result.sms?.let { SmsService.sendAsync(it.phone, it.message) }

        call.respond(HttpStatusCode.Created, result.response)
    }

    delete("{id}") {
        val tenant = call.currentTenant()
        val tenantSchema = tenant.tenantSchema

        val id = call.parameters["id"]?.toIntOrNull()

        if (id == null) {
            call.respond(HttpStatusCode.BadRequest, "invalid ID")
            return@delete
        }

        val ok = FamilyPaymentRepository.delete(
            tenantSchema = tenantSchema,
            id = id
        )

        if (!ok) {
            call.respond(HttpStatusCode.NotFound, "Family payment not found")
            return@delete
        } else {
            call.respond(HttpStatusCode.NoContent)
        }
    }



}
