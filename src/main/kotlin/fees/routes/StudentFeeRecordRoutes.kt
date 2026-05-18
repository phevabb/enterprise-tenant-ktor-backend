package com.example.fees.routes

import io.ktor.server.routing.Route

import com.example.fees.dtos.requests.AddPaymentRequest
import com.example.fees.dtos.requests.CreateStudentFeeRecordRequest
import com.example.fees.dtos.responses.ArrearsResponse
import com.example.fees.repos.StudentFeeRecordRepository
import com.example.student.dtos.PaginatedResponse
import com.example.student.dtos.PaginationMeta
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.studentFeeRecordRoutes() {

    /**
     * GET /api/student-fee-record
     * Optional query: ?studentId=123  -> returns records for student
     */
    get {
        val result = StudentFeeRecordRepository.findAll()
        call.respond(HttpStatusCode.OK, result)
    }

    get("/paginated") {
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
        val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20

        val search = call.request.queryParameters["search"]?.trim()
        val feeStructureId = call.request.queryParameters["fee_structure_id"]?.toIntOrNull()
        val isFullyPaid = call.request.queryParameters["is_fully_paid"]?.trim()?.lowercase()?.toBooleanStrictOrNull()
        // is_fully_paid=true|false (anything else becomes null = ignore)

        val (records, total) = StudentFeeRecordRepository.findAllPaginated(
            page = page,
            limit = limit,
            search = search,
            feeStructureId = feeStructureId,
            isFullyPaid = isFullyPaid
        )

        val response = PaginatedResponse(
            data = records,
            meta = PaginationMeta(
                page = page,
                limit = limit,
                total = total,
                totalPages = ((total + limit - 1) / limit).toInt()
            )
        )

        call.respond(HttpStatusCode.OK, response)
    }





    get("{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid id")

        val record = StudentFeeRecordRepository.findById(id)
            ?: return@get call.respond(HttpStatusCode.NotFound, "Student fee record not found")

        call.respond(HttpStatusCode.OK, record)
    }

    /**
     * POST /api/student-fee-record
     * Body: { "studentId": 1, "feeStructureId": 10 }
     */
    post {
        val req = call.receive<CreateStudentFeeRecordRequest>()

        val created = try {
            StudentFeeRecordRepository.create(
                studentId = req.studentId,
                feeStructureId = req.feeStructureId
            )
        } catch (e: Exception) {
            println("bug is $e")
            return@post call.respond(
                HttpStatusCode.BadRequest,
                e.message ?: "Failed to create student fee record"
            )
        }

        call.respond(HttpStatusCode.Created, created)
    }

    /**
     * PATCH /api/student-fee-record/{id}/payment
     * Body: { "payment": 100 }
     */
    patch("{id}/payment") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@patch call.respond(HttpStatusCode.BadRequest, "Invalid id")

        val req = call.receive<AddPaymentRequest>()

        val updated = StudentFeeRecordRepository.addPayment(id, req.payment)
            ?: return@patch call.respond(HttpStatusCode.NotFound, "Student fee record not found")

        call.respond(HttpStatusCode.OK, updated)
    }

    /**
     * GET /api/student-fee-record/arrears?studentId=1&academicYearId=5
     * Optional: &excludeRecordId=12
     */
    get("arrears") {
        val studentId = call.request.queryParameters["studentId"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, "studentId is required")

        val academicYearId = call.request.queryParameters["academicYearId"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, "academicYearId is required")

        val excludeId = call.request.queryParameters["excludeRecordId"]?.toIntOrNull()

        val total = StudentFeeRecordRepository.totalArrears(
            studentId = studentId,
            academicYearId = academicYearId,
            excludeRecordId = excludeId
        )

        call.respond(HttpStatusCode.OK, ArrearsResponse(studentId, academicYearId, total))
    }

    /**
     * DELETE /api/student-fee-record/{id}
     */
    delete("{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid id")

        val ok = StudentFeeRecordRepository.delete(id)

        if (!ok) {
            call.respond(HttpStatusCode.NotFound, "Student fee record not found")
        } else {
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
