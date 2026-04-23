package com.example.fees.routes

import com.example.fees.dtos.requests.CreateFeeStructureRequest
import com.example.fees.dtos.requests.PatchFeeStructureRequest
import com.example.fees.repos.FeeStructureRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.patch
import io.ktor.server.routing.post

fun Route.feeStructureRoutes(){

    get{
        val feeStructures = FeeStructureRepository.findAll()
        call.respond(HttpStatusCode.OK, feeStructures)
    }

    post {
//      val rawbody = call.receiveText()
//        println(rawbody)
        val req = call.receive<CreateFeeStructureRequest>()
        println("new request is $req")


        val c = FeeStructureRepository.create(req.academic_year_id,req.grade_class_id,req.term_id,req.amount,req.is_discounted)

        call.respond(HttpStatusCode.Created, c)

    }

    delete("{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
        if (id == null){
            call.respond(HttpStatusCode.BadRequest, "Invalid id")
            return@delete
        }
        val ok = FeeStructureRepository.delete(id)
        if(!ok){
            call.respond(HttpStatusCode.NotFound, "Fee structure Not found")
            return@delete
        }else{
            call.respond(HttpStatusCode.NoContent)
        }
    }

    patch("{id}") {

        val id = call.parameters["id"]?.toIntOrNull()
        if(id == null){
            call.respond(HttpStatusCode.BadRequest, "Invalid id")
            return@patch
        }
        val req = call.receive<PatchFeeStructureRequest>()
        val updated = FeeStructureRepository.patch(id, req.amount, req.is_discounted)
        if(updated == null){
            call.respond(HttpStatusCode.BadRequest, "Invalid id")
        }else  {
            call.respond(HttpStatusCode.OK, updated)
        }



    }



}