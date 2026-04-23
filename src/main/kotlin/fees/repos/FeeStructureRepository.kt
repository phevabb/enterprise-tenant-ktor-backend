package com.example.fees.repos

import com.example.fees.dtos.responses.FeeStructureResponseDto
import com.example.fees.dtos.responses.toFeeStructureResponseDto
import com.example.fees.mappers.toFeeStructureModel

import com.example.fees.models.FeeStructureModel
import com.example.fees.tables.FeeStructureTable
import com.example.student.tables.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction


object FeeStructureRepository {

    // ✅ CREATE
    fun create(

        academicYearId: Int,
        gradeClassId: Int,
        termId: Int,
        amount: Int,
        isDiscounted: Boolean
    ) = transaction {

        val id = FeeStructureTable.insertAndGetId {
            it[academic_year] = EntityID(academicYearId, AcademicYearTable)
            it[grade_class] = EntityID(gradeClassId, NewGradeClassTable)
            it[term] = EntityID(termId, TermTable)
            it[FeeStructureTable.amount] = amount
            it[is_discounted] = isDiscounted
        }.value

        findByIdWithRelations(id)
            ?: error("FeeStructure created but not found")
    }

    // ✅ FIND BY ID (with joins)
    fun findByIdWithRelations(id: Int): FeeStructureResponseDto? = transaction {

        FeeStructureTable
            .join(AcademicYearTable, JoinType.INNER, FeeStructureTable.academic_year, AcademicYearTable.id)
            .join(NewGradeClassTable, JoinType.INNER, FeeStructureTable.grade_class, NewGradeClassTable.id)
            .join(TermTable, JoinType.INNER, FeeStructureTable.term, TermTable.id)
            .selectAll()
            .where { FeeStructureTable.id eq id }
            .singleOrNull()
            ?.toFeeStructureResponseDto()
    }

    // ✅ LIST ALL
    fun findAll(): List<FeeStructureResponseDto> = transaction {

        FeeStructureTable
            .join(AcademicYearTable, JoinType.INNER, FeeStructureTable.academic_year, AcademicYearTable.id)
            .join(NewGradeClassTable, JoinType.INNER, FeeStructureTable.grade_class, NewGradeClassTable.id)
            .join(TermTable, JoinType.INNER, FeeStructureTable.term, TermTable.id)
            .selectAll()
            .orderBy(FeeStructureTable.id, SortOrder.DESC)
            .map { it.toFeeStructureResponseDto() }
    }

    // ✅ PATCH (partial update)
    fun patch(
        id: Int,
        amount: Int?,
        isDiscounted: Boolean?
    ): FeeStructureModel? = transaction {

        val updated = FeeStructureTable.update(
            where = { FeeStructureTable.id eq id }
        ) {
            amount?.let { amt -> it[FeeStructureTable.amount] = amt }
            isDiscounted?.let { d -> it[is_discounted] = d }
        }

        if (updated == 0) null else
            FeeStructureTable
                .selectAll()
                .where { FeeStructureTable.id eq id }
                .single()
                .toFeeStructureModel()
    }

    // ✅ DELETE
    fun delete(id: Int): Boolean = transaction {
        FeeStructureTable.deleteWhere { FeeStructureTable.id eq id } > 0
    }
}