package com.example.student.routes

import com.example.student.dtos.requests.CreateTermIncoming
import com.example.student.dtos.requests.CreateTermRequest
import com.example.student.dtos.requests.PatchTermRequest
import com.example.student.models.TermModel
import com.example.student.repos.TermRepository
import com.example.student.tables.TermTable
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import org.jetbrains.exposed.sql.selectAll
import io.ktor.server.response.*
import io.ktor.server.routing.delete
import io.ktor.server.routing.patch
import io.ktor.server.routing.post

fun Route.termRoutes() {

    authenticate("auth-jwt") {
    get {
        val terms = TermRepository.findAllWithYearName()
        call.respond(HttpStatusCode.OK, terms)
    }

    post {
        val incoming = call.receive<CreateTermIncoming>()

        val req = CreateTermRequest(
            name = incoming.name,
            academic_year = incoming.academic_year_id.toInt()
        )

        if (req.academic_year == null) {
            call.respond(HttpStatusCode.BadRequest, "academic_year is required")
            return@post
        }

        val term = TermModel(
            id = 0, // ignored, DB generates it
            name = req.name.trim(),
            academic_year = req.academic_year
        )

        val createdTerm = TermRepository.create(term)

        call.respond(HttpStatusCode.Created, createdTerm)
    }

    delete("{id}") {
        val id = call.parameters["id"]!!.toIntOrNull()

        if (id == null) {
            call.respond(HttpStatusCode.BadRequest, "id is required")
            return@delete
        }

        val ok = TermRepository.delete(id)
        if (!ok) {
            call.respond(HttpStatusCode.NotFound, "couldn't delete term")
        } else {
            call.respond(HttpStatusCode.NoContent)
        }
    }


    patch("{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@patch call.respond(HttpStatusCode.BadRequest, "Invalid id")

        val req = call.receive<PatchTermRequest>()

        val updated = TermRepository.patch(id, req)
            ?: return@patch call.respond(HttpStatusCode.NotFound, "Term not found")

        call.respond(HttpStatusCode.OK, updated)
    }


        get("current") {
            val result = TermRepository.getCurrent_()

            if (result == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "No term found")
                )
                return@get
            }

            call.respond(HttpStatusCode.OK, result)
        }

}

}





