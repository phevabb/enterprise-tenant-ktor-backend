package com.example.academics.repos




import com.example.academics.dtos.requests.CreateGradeRequest
import com.example.academics.dtos.requests.PatchGradeRequest
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

    fun patch(id: Int, req: PatchGradeRequest): Grade? = transaction {

            val existing = findById(id) ?: return@transaction null

            // ✅ compute what the new values would be (keep old if not provided)
            val newCode = (req.code ?: existing.code).trim().uppercase()
            val newLabel = (req.label ?: existing.label).trim()
            val newMin = req.minScore ?: existing.minScore
            val newMax = req.maxScore ?: existing.maxScore
            val newOrder = req.order ?: existing.order

            // ✅ basic validations
            if (newCode.isBlank()) return@transaction null
            if (newLabel.isBlank()) return@transaction null
            if (newMin > newMax) return@transaction null

            // ✅ uniqueness check for code only if code changed
            if (newCode != existing.code) {
                val dup = findByCode(newCode)
                if (dup != null) return@transaction null
            }

            GradesTable.update({ GradesTable.id eq id }) {
                it[code] = newCode
                it[label] = newLabel
                it[minScore] = newMin
                it[maxScore] = newMax
                it[order] = newOrder
            }

            findById(id)
        }

}
