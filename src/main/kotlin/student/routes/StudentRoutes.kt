package com.example.student.routes

import com.example.commands.ImportStudentsFromCsv
import com.example.student.dtos.PaginatedResponse
import com.example.student.dtos.PaginationMeta
import com.example.student.dtos.requests.CreateStudentRequest
import com.example.student.dtos.requests.PatchStudentRequest
import com.example.student.dtos.requests.UpdateStudentRequest
import com.example.student.repos.StudentRepository
import com.example.student.services.StudentService
import com.example.tenant.currentTenant
import io.ktor.http.*
import io.ktor.server.auth.authenticate
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.studentRoutes() {



// ✅ Raw endpoint (no pagination)
    authenticate("auth-jwt") {


        post("/import-students") {
            val tenant = call.currentTenant()

            ImportStudentsFromCsv.run(tenant.tenantSchema)

            call.respondText("Import started")
        }


            get("/raw") {
            val tenant = call.currentTenant()
            if (tenant == null) {
                call.respond(HttpStatusCode.NotFound, "Tenant not found")
                return@get
            }

            val tenantSchema = tenant.tenantSchema
            val search = call.request.queryParameters["search"]

            val students = StudentRepository.findAllWithUserAndClassRaw(
                tenantSchema, search

            )

            call.respond(HttpStatusCode.OK, students)
        }


        get("/number") {
            val tenant = call.currentTenant()
            if (tenant == null) {
                call.respond(HttpStatusCode.NotFound, "Tenant not found")
                return@get
            }

            val tenantSchema = tenant.tenantSchema

            val count = StudentRepository.countStudents(tenantSchema)

            call.respond(HttpStatusCode.OK, mapOf("count" to count))
        }


        get("/paginated") {
            val tenant = call.currentTenant()
            if (tenant == null) {
                call.respond(HttpStatusCode.NotFound, "Tenant not found")
                return@get
            }

            val tenantSchema = tenant.tenantSchema

            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val limit = 10
            val search = call.request.queryParameters["search"]

            val (students, total) =
                StudentRepository.findAllWithUserAndClass(
                    tenantSchema,
                    page,
                    limit,
                    search
                )

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
            val tenant = call.currentTenant()
            if (tenant == null) {
                call.respond(HttpStatusCode.NotFound, "Tenant not found")
                return@post
            }

            val tenantSchema = tenant.tenantSchema

            val req = call.receive<CreateStudentRequest>()

            val createdProfile = StudentService.createStudent(
                tenantSchema,
                req
            )

            call.respond(HttpStatusCode.Created, createdProfile)
        }

    // PUT /student/{id}  (full update)
        put("{id}") {
            val tenant = call.currentTenant()
            if (tenant == null) {
                call.respond(HttpStatusCode.NotFound, "Tenant not found")
                return@put
            }

            val tenantSchema = tenant.tenantSchema

            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid id")
                return@put
            }

            val req = call.receive<UpdateStudentRequest>()

            val ok = StudentService.updateStudent(
                tenantSchema,
                id,
                req
            )

            if (!ok) {
                call.respond(HttpStatusCode.NotFound, "Student not found")
            } else {
                call.respond(HttpStatusCode.OK, mapOf("message" to "Updated"))
            }
        }

    // DELETE /student/{id}
        delete("{id}") {
            val tenant = call.currentTenant()
            if (tenant == null) {
                call.respond(HttpStatusCode.NotFound, "Tenant not found")
                return@delete
            }

            val tenantSchema = tenant.tenantSchema

            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid id")
                return@delete
            }

            val ok = StudentRepository.delete(
                tenantSchema,
                id
            )

            if (!ok) {
                call.respond(HttpStatusCode.NotFound, "Student not found")
            } else {
                call.respond(HttpStatusCode.NoContent)
            }
        }

        patch("{id}") {
            val tenant = call.currentTenant()
            if (tenant == null) {
                call.respond(HttpStatusCode.NotFound, "Tenant not found")
                return@patch
            }

            val tenantSchema = tenant.tenantSchema

            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid id"))
                return@patch
            }

            val req = call.receive<PatchStudentRequest>()

            val updated = StudentRepository.patchNested(
                tenantSchema,
                id,
                req
            )

            if (updated == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Student not found"))
            } else {
                call.respond(HttpStatusCode.OK, updated)
            }
        }

        get("/per-class") {
            val tenant = call.currentTenant()
            if (tenant == null) {
                call.respond(HttpStatusCode.NotFound, "Tenant not found")
                return@get
            }

            val tenantSchema = tenant.tenantSchema

            val data = StudentRepository.countPerClass(tenantSchema)

            call.respond(HttpStatusCode.OK, data)
        }


    }

}





