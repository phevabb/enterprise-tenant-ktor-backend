package com.example.familyfees.routes

import com.example.familyfees.dtos.requests.CreateFamilyRequest
import com.example.familyfees.repos.FamilyRepository
import com.example.student.dtos.PaginationMeta
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import com.example.student.dtos.PaginatedResponse
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.patch
import io.ktor.server.routing.put

fun Route.familyRoutes() {

    authenticate("auth-jwt") {
        get {
            val allFamilies = FamilyRepository.findAll()
            call.respond(HttpStatusCode.OK, allFamilies)
        }



        get("/paginated") {
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
            val search = call.request.queryParameters["search"]?.trim()

            val (families, total) =
                FamilyRepository.findAllPaginated(page, limit, search)

            val response = PaginatedResponse(
                data = families,
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
            if (!ok) {
                call.respond(HttpStatusCode.NotFound, "Family not found")
                return@delete
            } else {
                call.respond(HttpStatusCode.NoContent)
            }

        }

        patch("{id}") {

            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid id")
                return@patch
            }

            val req = call.receive<CreateFamilyRequest>()

            val updated = FamilyRepository.update(id, req.name)

            if (updated == null) {
                call.respond(HttpStatusCode.NotFound, "Family not found")
            } else {
                call.respond(HttpStatusCode.OK, updated)
            }
        }

    }


}