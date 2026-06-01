package com.example.principal.service





import com.example.familyfees.tables.FamilyFeeRecordTable
import org.jetbrains.exposed.sql.*

import org.jetbrains.exposed.sql.transactions.transaction



import com.example.fees.tables.FeeStructureTable
import com.example.fees.tables.StudentFeeRecordTable
import com.example.staff.dtos.response.CollectionSummaryResponse
import com.example.student.tables.AcademicYearTable
import com.example.student.tables.TermTable
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.alias



import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.transactions.transaction

class CollectionSummaryService {

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
}