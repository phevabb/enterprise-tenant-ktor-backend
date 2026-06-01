package com.example.principal.routes





import com.example.admin.repos.AdminRepository
import com.example.familyfees.tables.FamilyFeeRecordTable
import com.example.fees.tables.FeeStructureTable
import com.example.fees.tables.StudentFeeRecordTable
import com.example.principal.dtos.requests.CreatePrincipalRequest
import com.example.principal.dtos.requests.PatchPrincipalRequest
import com.example.principal.dtos.responses.ExpectedFeesResponse
import com.example.principal.service.PrincipalService
import com.example.principal.repos.PrincipalRepository
import com.example.principal.repos.PrincipalRepository.countAdmins
import com.example.principal.repos.PrincipalRepository.countStaff

import com.example.principal.service.PrincipalService.expectedFeesSummary
import com.example.staff.dtos.response.CollectionSummaryResponse

import com.example.student.tables.AcademicYearTable
import com.example.student.tables.NewGradeClassTable
import com.example.student.tables.TermTable

import io.ktor.http.*
import io.ktor.server.auth.authenticate
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.sum
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



        get("/collection-summary") {
            val response = getCollectionSummary()
            call.respond(HttpStatusCode.OK, response)
        }

        get("/staff-number") {
            val count = countStaff()
            call.respond(HttpStatusCode.OK, mapOf("count" to count))
        }

        get("/admin-number") {
            val count = countAdmins()
            call.respond(HttpStatusCode.OK, mapOf("count" to count))}




    }}


private data class Totals(
    var collectedAmount: Int = 0,
    var pendingAmount: Int = 0
)

fun getCollectionSummary(): List<CollectionSummaryResponse> = transaction {
    val combined = linkedMapOf<Pair<String, String>, Totals>()

    // -----------------------------------------
    // Student totals
    // -----------------------------------------
    val studentCollectedSum = StudentFeeRecordTable.amountPaid.sum().alias("student_collected_sum")
    val studentPendingSum = StudentFeeRecordTable.balance.sum().alias("student_pending_sum")

    val studentSummary = StudentFeeRecordTable
        .join(
            otherTable = FeeStructureTable,
            joinType = JoinType.INNER,
            onColumn = StudentFeeRecordTable.feeStructure,
            otherColumn = FeeStructureTable.id
        )
        .join(
            otherTable = TermTable,
            joinType = JoinType.INNER,
            onColumn = FeeStructureTable.term,
            otherColumn = TermTable.id
        )
        .join(
            otherTable = AcademicYearTable,
            joinType = JoinType.INNER,
            onColumn = FeeStructureTable.academic_year,
            otherColumn = AcademicYearTable.id
        )
        .select(
            AcademicYearTable.name,
            TermTable.name,
            studentCollectedSum,
            studentPendingSum
        )
        .groupBy(AcademicYearTable.name, TermTable.name)
        .map { row ->
            val academicYear = row[AcademicYearTable.name]
            val term = row[TermTable.name]

            val collected = row[studentCollectedSum]?.toInt() ?: 0
            val pending = row[studentPendingSum]?.toInt() ?: 0

            Triple(academicYear, term, collected to pending)
        }

    studentSummary.forEach { (academicYear, term, amounts) ->
        val key = academicYear to term
        val totals = combined.getOrPut(key) { Totals() }

        totals.collectedAmount += amounts.first
        totals.pendingAmount += amounts.second
    }

    // -----------------------------------------
    // Family totals
    // -----------------------------------------
    val familyCollectedSum = FamilyFeeRecordTable.amount_paid.sum().alias("family_collected_sum")
    val familyPendingSum = FamilyFeeRecordTable.balance.sum().alias("family_pending_sum")

    val familySummary = FamilyFeeRecordTable
        .join(
            otherTable = TermTable,
            joinType = JoinType.INNER,
            onColumn = FamilyFeeRecordTable.term,
            otherColumn = TermTable.id
        )
        .join(
            otherTable = AcademicYearTable,
            joinType = JoinType.INNER,
            onColumn = FamilyFeeRecordTable.academic_year,
            otherColumn = AcademicYearTable.id
        )
        .select(
            AcademicYearTable.name,
            TermTable.name,
            familyCollectedSum,
            familyPendingSum
        )
        .groupBy(AcademicYearTable.name, TermTable.name)
        .map { row ->
            val academicYear = row[AcademicYearTable.name]
            val term = row[TermTable.name]

            val collected = row[familyCollectedSum]?.toInt() ?: 0
            val pending = row[familyPendingSum]?.toInt() ?: 0

            Triple(academicYear, term, collected to pending)
        }

    familySummary.forEach { (academicYear, term, amounts) ->
        val key = academicYear to term
        val totals = combined.getOrPut(key) { Totals() }

        totals.collectedAmount += amounts.first
        totals.pendingAmount += amounts.second
    }

    combined.entries
        .map { (key, value) ->
            CollectionSummaryResponse(
                academicYear = key.first,
                term = key.second,
                collectedAmount = value.collectedAmount,
                pendingAmount = value.pendingAmount
            )
        }
        .sortedWith(compareBy({ it.academicYear }, { it.term }))
}

