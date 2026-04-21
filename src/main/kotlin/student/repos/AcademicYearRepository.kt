package com.example.student.repos

import com.example.student.dtos.requests.PatchAcademicYearRequest
import com.example.student.mappers.toAcademicYearModel
import com.example.student.models.AcademicYearModel
import com.example.student.tables.AcademicYearTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

object AcademicYearRepository {

    fun create(name: String): AcademicYearModel = transaction {
        val id = AcademicYearTable.insertAndGetId {
            it[AcademicYearTable.name] = name
        }.value

        AcademicYearModel(
            id = id,
            name = name,
        )
    }


    fun findAll(): List<AcademicYearModel> = transaction {
        AcademicYearTable
            .selectAll()
            .orderBy(AcademicYearTable.id, SortOrder.DESC)
            .map {it.toAcademicYearModel()}
    }

    fun delete(id: Int) = transaction {
        AcademicYearTable.deleteWhere { AcademicYearTable.id eq id }
    } > 0

    fun findById(id: Int): AcademicYearModel? = transaction {
        AcademicYearTable
            .selectAll()
            .where{ AcademicYearTable.id eq id }
            .singleOrNull()
        ?.toAcademicYearModel()
    }

    fun patch(id: Int, req: PatchAcademicYearRequest): AcademicYearModel? = transaction {
        val rowUpdated = AcademicYearTable.update(
            where = { AcademicYearTable.id eq id}
        ){ row ->
            req.name?.let {row[AcademicYearTable.name] = it }
        }

        if(rowUpdated == 0) {
            null
        }else {
            findById(id)!!
        }
        }
    }

