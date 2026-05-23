package com.example.academics.routes


import com.example.academics.dtos.requests.CreateAcademicRecordRequest
import com.example.academics.repos.AcademicRecordRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.academicRecordRoutes() {

    // ✅ GET ALL
    get {
        val records = AcademicRecordRepository.findAll()
        call.respond(HttpStatusCode.OK, records)
    }

    // ✅ CREATE / GET OR CREATE
    post("/get-or-create") {

        val req = call.receive<CreateAcademicRecordRequest>()

        val record = AcademicRecordRepository.getOrCreate(
            studentId = req.studentId,
            termId = req.termId,
            academicYearId = req.academicYearId,
            classLevelId = req.classLevelId
        )

        call.respond(HttpStatusCode.OK, record)
    }
}