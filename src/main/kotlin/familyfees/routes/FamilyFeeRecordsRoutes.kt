package com.example.familyfees.routes

import com.example.familyfees.dtos.requests.CreateFamilyFeeRecordsRequests
import com.example.familyfees.repos.FamilyFeeRecordsRepository
import com.example.familyfees.repos.FamilyRepository
import com.example.student.dtos.PaginatedResponse
import com.example.student.dtos.PaginationMeta
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.request.receive
import io.ktor.server.routing.delete
import kotlin.text.toIntOrNull

fun Route.familyFeeRecordsRoutes() {


    authenticate("auth-jwt") {
        get {
            val allRecords = FamilyFeeRecordsRepository.findAll()
            call.respond(HttpStatusCode.OK, allRecords)
        }

        get("/paginated") {

            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
            val search = call.request.queryParameters["search"]?.trim()

            val (records, total) =
                FamilyFeeRecordsRepository.findAllPaginated(page, limit, search)

            val response = PaginatedResponse(
                data = records,
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
            val req = call.receive<CreateFamilyFeeRecordsRequests>()
            val created = FamilyFeeRecordsRepository.create(
                family = req.family,
                term = req.term,
                academic_year = req.academic_year,
                amount_to_pay = req.amount_to_pay,
                amount_paid = 0
            )
            call.respond(HttpStatusCode.Created, created)
        }

        delete("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "invalid ID")
                return@delete
            }
            val ok = FamilyFeeRecordsRepository.delete(id)
            if (!ok) {
                call.respond(HttpStatusCode.NotFound, "Family fee rec. not found")
                return@delete
            } else {
                call.respond(HttpStatusCode.NoContent)
            }

        }

    }


}