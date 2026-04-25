package com.example.familyfees.routes

import com.example.familyfees.dtos.requests.CreateFamilyRequest
import com.example.familyfees.repos.FamilyRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.post

fun Route.familyRoutes() {

    get{
        val allFamilies = FamilyRepository.findAll()
        call.respond(HttpStatusCode.OK, allFamilies)
    }

    post{
        val req = call.receive<CreateFamilyRequest>()
        val created = FamilyRepository.create(req.name)
        call.respond(HttpStatusCode.Created, created)
    }

    delete("{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
        if (id == null) {
            call.respond(HttpStatusCode.BadRequest, "invalid ID")
            return@delete
        }
        val ok = FamilyRepository.delete(id)
        if(!ok){
            call.respond(HttpStatusCode.NotFound, "Family not found")
            return@delete
        }

        else  {
            call.respond(HttpStatusCode.NoContent)
        }

    }


}