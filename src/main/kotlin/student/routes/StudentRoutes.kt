package com.example.student.routes

import com.example.student.dtos.PaginatedResponse
import com.example.student.dtos.PaginationMeta
import com.example.student.dtos.requests.CreateStudentRequest
import com.example.student.dtos.requests.PatchStudentRequest
import com.example.student.dtos.requests.UpdateStudentRequest
import com.example.student.repos.StudentRepository
import com.example.student.services.StudentService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.studentRoutes() {



// ✅ Raw endpoint (no pagination)


    get ("/raw"){
        val search = call.request.queryParameters["search"]
        val students = StudentRepository.findAllWithUserAndClassRaw(search)
        call.respond(HttpStatusCode.OK, students)
    }





    get("/paginated") {
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
        val limit = 10
        val search = call.request.queryParameters["search"]

        val (students, total) =
            StudentRepository.findAllWithUserAndClass(page, limit, search)

        val response = PaginatedResponse(
            data = students,
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


        // 1) Receive JSON body from frontend into your DTO
        val req = call.receive<CreateStudentRequest>()
        println("the structure frontend is $req")


        // 2) Call service (your DRF serializer.create() equivalent)
        val createdProfile = StudentService.createStudent(req)

        // 3) Respond with Created + the created profile (as JSON)
        call.respond(HttpStatusCode.Created, createdProfile)
    }



    // PUT /student/{id}  (full update)
    put("{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
        if (id == null) {
            call.respond(HttpStatusCode.BadRequest, "Invalid id")
            return@put
        }

        val req = call.receive<UpdateStudentRequest>()
        val ok = StudentService.updateStudent(id, req)

        if (!ok) {
            call.respond(HttpStatusCode.NotFound, "Student not found")
        } else {
            call.respond(HttpStatusCode.OK, mapOf("message" to "Updated"))
        }
    }

    // DELETE /student/{id}
    delete("{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
        if (id == null) {
            call.respond(HttpStatusCode.BadRequest, "Invalid id")
            return@delete
        }

        val ok = StudentRepository.delete(id)

        if (!ok) {
            call.respond(HttpStatusCode.NotFound, "Student not found")
        } else {
            call.respond(HttpStatusCode.NoContent)
        }
    }


    patch("{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
        if (id == null) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid id"))
            return@patch
        }
        val req = call.receive<PatchStudentRequest>()
        val updated = StudentRepository.patchNested(id, req)

        if (updated == null) {
            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Student not found"))
        } else {
            call.respond(HttpStatusCode.OK, updated)
        }
    }



}





