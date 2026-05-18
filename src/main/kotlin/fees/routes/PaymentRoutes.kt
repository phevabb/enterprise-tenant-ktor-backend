package com.example.fees.routes

import com.example.fees.dtos.requests.CreatePaymentRequest
import com.example.student.dtos.PaginatedResponse
import com.example.student.dtos.PaginationMeta
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


    get("/paginated") {
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
        val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
        val search = call.request.queryParameters["search"]?.trim()
        val dateFilter = call.request.queryParameters["date_filter"]?.trim() // today|7days|month|year

        val (payments, total) = PaymentRepository.findAllPaginated(
            page = page,
            limit = limit,
            search = search,
            dateFilter = dateFilter
        )

        val response = PaginatedResponse(
            data = payments,
            meta = PaginationMeta(
                page = page,
                limit = limit,
                total = total,
                totalPages = ((total + limit - 1) / limit).toInt()
            )
        )

        call.respond(HttpStatusCode.OK, response)
    }

    post {
        val req = call.receive<CreatePaymentRequest>()

        val result = PaymentRepository.createPaymentAndUpdateSfr(
            studentFeeRecordId = req.student_fee_record_id,
            amount = req.amount,
            paymentMethod = "cash"
        )

        println("results.sms is ${result.sms}")
        // ✅ send only after transaction succeeded
        result.sms?.let { SmsService.sendAsync(it.phone, it.message) }

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