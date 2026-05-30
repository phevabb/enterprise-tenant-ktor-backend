package com.example.principal.routes





import com.example.principal.dtos.requests.CreatePrincipalRequest
import com.example.principal.dtos.requests.PatchPrincipalRequest
import com.example.principal.dtos.responses.ExpectedFeesResponse
import com.example.principal.service.PrincipalService
import com.example.principal.repos.PrincipalRepository
import com.example.principal.service.PrincipalService.expectedFeesSummary
import com.example.student.tables.AcademicYearTable
import com.example.student.tables.NewGradeClassTable

import io.ktor.http.*
import io.ktor.server.auth.authenticate
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.principalRoutes() {

    authenticate("auth-jwt") {

        get("/raw") {
            val search = call.request.queryParameters["search"]
            val data = PrincipalRepository.findAllWithUser(search)
            call.respond(HttpStatusCode.OK, data)
        }

        post {
            val req = call.receive<CreatePrincipalRequest>()
            val created = PrincipalService.createPrincipal(req)
            call.respond(HttpStatusCode.Created, created)
        }

        patch("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()

            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid id"))
                return@patch
            }

            val req = call.receive<PatchPrincipalRequest>()
            val updated = PrincipalRepository.patchNested(id, req)

            if (updated == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Principal not found"))
            } else {
                call.respond(HttpStatusCode.OK, updated)
            }
        }

        delete("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()

            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid id")
                return@delete
            }

            val ok = PrincipalRepository.delete(id)

            if (!ok) {
                call.respond(HttpStatusCode.NotFound, "Principal not found")
            } else {
                call.respond(HttpStatusCode.NoContent)
            }
        }

        get("/fees/expected_fees") {
            val data = expectedFeesSummary()
            call.respond(HttpStatusCode.OK, data)


    }



    }}
