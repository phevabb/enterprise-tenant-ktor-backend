package com.example.student.routes

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

    get {

        val students = StudentRepository.findAllWithUserAndClass()
        call.respond(HttpStatusCode.OK, students)

    }

    post {
        // 1) Receive JSON body from frontend into your DTO
        val req = call.receive<CreateStudentRequest>()


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

        val ok = StudentService.deleteStudent(id)

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





