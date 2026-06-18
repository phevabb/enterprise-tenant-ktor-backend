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

    fun create(
        tenantSchema: String,
        req: CreateGradeRequest
    ): Grade = transaction {

        setTenantSchema(tenantSchema)

        val id = GradesTable.insertAndGetId {
            it[code] = req.code.uppercase().trim()
            it[label] = req.label.trim()
            it[minScore] = req.minScore
            it[maxScore] = req.maxScore
            it[order] = req.order
        }.value

        findById(tenantSchema, id)!!
    }

    fun findAll(
        tenantSchema: String
    ): List<Grade> = transaction {

        setTenantSchema(tenantSchema)

        GradesTable
            .selectAll()
            .orderBy(GradesTable.order to SortOrder.ASC)
            .map { it.toGrade() }
    }

    fun findById(
        tenantSchema: String,
        id: Int
    ): Grade? = transaction {

        setTenantSchema(tenantSchema)

        GradesTable
            .selectAll()
            .where { GradesTable.id eq id }
            .singleOrNull()
            ?.toGrade()
    }

    fun findByCode(
        tenantSchema: String,
        code: String
    ): Grade? = transaction {

        setTenantSchema(tenantSchema)

        GradesTable
            .selectAll()
            .where { GradesTable.code eq code.uppercase() }
            .singleOrNull()
            ?.toGrade()
    }

    fun delete(
        tenantSchema: String,
        id: Int
    ): Boolean = transaction {

        setTenantSchema(tenantSchema)

        GradesTable.deleteWhere {
            GradesTable.id eq id
        } > 0
    }

    fun patch(
        tenantSchema: String,
        id: Int,
        req: PatchGradeRequest
    ): Grade? = transaction {

        setTenantSchema(tenantSchema)

        val existing = GradesTable
            .selectAll()
            .where { GradesTable.id eq id }
            .singleOrNull()
            ?.toGrade()
            ?: return@transaction null

        val newCode = (req.code ?: existing.code).trim().uppercase()
        val newLabel = (req.label ?: existing.label).trim()
        val newMin = req.minScore ?: existing.minScore
        val newMax = req.maxScore ?: existing.maxScore
        val newOrder = req.order ?: existing.order

        if (newCode.isBlank()) return@transaction null
        if (newLabel.isBlank()) return@transaction null
        if (newMin > newMax) return@transaction null

        if (newCode != existing.code) {

            val duplicate = GradesTable
                .selectAll()
                .where {
                    (GradesTable.code eq newCode) and
                            (GradesTable.id neq id)
                }
                .singleOrNull()

            if (duplicate != null) {
                return@transaction null
            }
        }

        GradesTable.update({ GradesTable.id eq id }) {
            it[code] = newCode
            it[label] = newLabel
            it[minScore] = newMin
            it[maxScore] = newMax
            it[order] = newOrder
        }

        GradesTable
            .selectAll()
            .where { GradesTable.id eq id }
            .singleOrNull()
            ?.toGrade()
    }

}
