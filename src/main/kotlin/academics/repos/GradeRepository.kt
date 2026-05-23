package com.example.academics.repos




import com.example.academics.dtos.requests.CreateGradeRequest
import com.example.academics.mappers.toGrade
import com.example.academics.models.Grade
import com.example.academics.tables.GradesTable

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object GradeRepository {

    fun create(req: CreateGradeRequest): Grade = transaction {

        val id = GradesTable.insertAndGetId {
            it[code] = req.code.uppercase().trim()
            it[label] = req.label.trim()
            it[minScore] = req.minScore
            it[maxScore] = req.maxScore
            it[order] = req.order
        }.value

        findById(id)!!
    }

    fun findAll(): List<Grade> = transaction {
        GradesTable
            .selectAll()
            .orderBy(GradesTable.order to SortOrder.ASC) // ✅ Django ordering
            .map { it.toGrade() }
    }

    fun findById(id: Int): Grade? = transaction {
        GradesTable
            .selectAll()
            .where { GradesTable.id eq id }
            .singleOrNull()
            ?.toGrade()
    }

    fun findByCode(code: String): Grade? = transaction {
        GradesTable
            .selectAll()
            .where { GradesTable.code eq code.uppercase() }
            .singleOrNull()
            ?.toGrade()
    }

    fun delete(id: Int): Boolean = transaction {
        GradesTable.deleteWhere { GradesTable.id eq id } > 0
    }
}
