package com.example.familyfees.repos

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
}