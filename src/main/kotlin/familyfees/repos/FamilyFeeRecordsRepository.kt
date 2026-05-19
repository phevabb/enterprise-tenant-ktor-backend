package com.example.familyfees.repos

import com.example.account.AccountTable
import com.example.familyfees.dtos.responses.FamilyFeeRecordsResponseDto
import com.example.familyfees.dtos.responses.toFamilyFeeRecordsResponseDto
import com.example.familyfees.tables.FamilyFeeRecordTable
import com.example.familyfees.tables.FamilyTable
import com.example.fees.tables.FeeStructureTable
import com.example.student.tables.AcademicYearTable
import com.example.student.tables.TermTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object FamilyFeeRecordsRepository {
    fun findById(id: Int): FamilyFeeRecordsResponseDto? = transaction {
        FamilyFeeRecordTable
            .join(AcademicYearTable, JoinType.INNER, FamilyFeeRecordTable.academic_year, AcademicYearTable.id )
            .join(FamilyTable, JoinType.INNER, FamilyFeeRecordTable.family, FamilyTable.id )
            .join(TermTable, JoinType.INNER, FamilyFeeRecordTable.term, TermTable.id)
            .selectAll()
            .where { FamilyFeeRecordTable.id eq id }
            .singleOrNull()
            ?.toFamilyFeeRecordsResponseDto()
    }

    fun create(
        family : Int,
        academic_year: Int,
        term : Int,
        amount_to_pay: Int,
        amount_paid : Int,

    )       = transaction {
        val id = FamilyFeeRecordTable.insertAndGetId {
            it[FamilyFeeRecordTable.family] = EntityID(family, FamilyTable)
            it[FamilyFeeRecordTable.academic_year] = EntityID(academic_year, AcademicYearTable)
            it[FamilyFeeRecordTable.term] = EntityID(term, TermTable)
            it[FamilyFeeRecordTable.amount_to_pay] = amount_to_pay
            it[FamilyFeeRecordTable.amount_paid] = amount_paid
            it[FamilyFeeRecordTable.balance] = amount_to_pay
            it[FamilyFeeRecordTable.date_created] = System.currentTimeMillis()

        }.value
        findById(id)?: error("record not found")
    }

    fun findAll(): List<FamilyFeeRecordsResponseDto> = transaction {
        FamilyFeeRecordTable
            .join(AcademicYearTable, JoinType.INNER, FamilyFeeRecordTable.academic_year, AcademicYearTable.id )
            .join(FamilyTable, JoinType.INNER, FamilyFeeRecordTable.family, FamilyTable.id )
            .join(TermTable, JoinType.INNER, FamilyFeeRecordTable.term, TermTable.id)
            .selectAll()
            .orderBy(FamilyFeeRecordTable.id, SortOrder.DESC)
            .map { it.toFamilyFeeRecordsResponseDto() }
    }

    fun delete(id: Int): Boolean = transaction {
        FamilyFeeRecordTable.deleteWhere { FamilyFeeRecordTable.id eq id } > 0
    }


    fun findAllPaginated(
        page: Int,
        limit: Int,
        search: String?
    ): Pair<List<FamilyFeeRecordsResponseDto>, Long> = transaction {

        val offset = ((page - 1) * limit).toLong()

        // ✅ FETCH ALL (SAFE VERSION)
        val rows = FamilyFeeRecordTable
            .join(
                FamilyTable,
                JoinType.INNER,
                additionalConstraint = { FamilyFeeRecordTable.family eq FamilyTable.id }
            )
            .join(
                TermTable,
                JoinType.INNER,
                additionalConstraint = { FamilyFeeRecordTable.term eq TermTable.id }
            )
            .join(
                AcademicYearTable,
                JoinType.INNER,
                additionalConstraint = { FamilyFeeRecordTable.academic_year eq AcademicYearTable.id }
            )
            .selectAll()
            .orderBy(FamilyFeeRecordTable.id, SortOrder.DESC)
            .toList()

        // ✅ CASE-INSENSITIVE SEARCH (KOTLIN SIDE)
        val filtered = if (!search.isNullOrBlank()) {
            val q = search.lowercase()

            rows.filter { r ->
                val familyName = r.getOrNull(FamilyTable.name)?.lowercase() ?: ""
                val studentName = r.getOrNull(AccountTable.fullName)?.lowercase() ?: ""

                familyName.contains(q) || studentName.contains(q)
            }
        } else rows

        val total = filtered.size.toLong()

        val paginated = filtered
            .drop(offset.toInt())
            .take(limit)

        val result = paginated.map { row ->
            row.toFamilyFeeRecordsResponseDto()
        }

        return@transaction Pair(result, total)
    }



}