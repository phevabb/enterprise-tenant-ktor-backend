package com.example.student.routes

import com.example.academics.repos.NewAcademicYearRepository
import com.example.student.dtos.requests.CreateAcademicYearRequest
import com.example.student.dtos.requests.PatchAcademicYearRequest
import com.example.student.repos.AcademicYearRepository
import com.example.student.tables.NewGradeClassTable
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.response.*
import io.ktor.server.routing.delete
import io.ktor.server.routing.patch
import io.ktor.server.routing.post

fun Route.academicYearRoutes() {
    authenticate("auth-jwt") {
        get {
            val years = AcademicYearRepository.findAll()

            call.respond(HttpStatusCode.OK, years)
        }

        post {
            val rawBody = call.receiveText()
            println("RAW BODY = $rawBody")

            // rawBody is: "2027/2028"
            val yearValue = rawBody.removeSurrounding("\"")

            val req = CreateAcademicYearRequest(
                name = yearValue
            )

            val created = AcademicYearRepository.create(
                name = req.name.trim()
            )

            call.respond(HttpStatusCode.Created, created)
        }

        delete("{id}") {
            val id = call.parameters["id"]!!.toIntOrNull()

            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "invalid id")
                return@delete
            }

            val ok = AcademicYearRepository.delete(id)
            if (!ok) {
                call.respond(HttpStatusCode.NotFound, "year not found")
            } else {
                call.respond(HttpStatusCode.NoContent)
            }
        }

        patch("{id}") {
            val rawBody = call.receiveText()
            println("RAW BODY to patch = $rawBody")
            val yearValue = rawBody.removeSurrounding("\"")

            val id = call.parameters["id"]!!.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "invalid id")
                return@patch
            }

            val req = PatchAcademicYearRequest(
                name = yearValue
            )

            val updated = AcademicYearRepository.patch(id, req)
            if (updated == null) {
                call.respond(HttpStatusCode.BadRequest, "invalid id")
                return@patch
            } else {
                call.respond(HttpStatusCode.OK, updated)
            }


        }


        get("all-years") {
            val years = NewAcademicYearRepository.findAll()
            call.respond(HttpStatusCode.OK, years)
        }

    }
}