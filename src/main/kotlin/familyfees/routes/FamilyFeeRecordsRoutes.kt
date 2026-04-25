package com.example.familyfees.routes

import com.example.familyfees.dtos.requests.CreateFamilyFeeRecordsRequests
import com.example.familyfees.repos.FamilyFeeRecordsRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.request.receive

fun Route.familyFeeRecordsRoutes() {

    get{
        val allRecords = FamilyFeeRecordsRepository.findAll()
        call.respond(HttpStatusCode.OK, allRecords)
    }

    post{
        val req = call.receive<CreateFamilyFeeRecordsRequests>()
        val created = FamilyFeeRecordsRepository.create(family=req.family, term=req.term, academic_year=req.academic_year, amount_to_pay=req.amount_to_pay, amount_paid=req.amount_paid  )
        call.respond(HttpStatusCode.Created, created)
    }
}