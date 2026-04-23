package com.example.student.repos
import com.example.student.dtos.requests.PatchTermRequest
import com.example.student.dtos.response.AcademicYearResponse
import org.jetbrains.exposed.sql.SortOrder
import com.example.student.mappers.toTermModel
import com.example.student.models.TermModel
import com.example.student.tables.TermTable
import org.jetbrains.exposed.dao.id.EntityID

import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import com.example.student.dtos.response.TermResponseDto
import com.example.student.tables.AcademicYearTable
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.update

object TermRepository {

    fun findAll(): List<TermModel> = transaction {
        TermTable
            .selectAll()
            .orderBy(TermTable.id, SortOrder.DESC)
            .map { it.toTermModel() }
    }

    fun create(term: TermModel): TermResponseDto = transaction {

        val id = TermTable.insertAndGetId {
            it[name] = term.name
            it[academic_year] = EntityID(term.academic_year, AcademicYearTable)
        }.value

        findByIdWithYearName(id)
            ?: error("Term created but not found")
    }

    fun findByIdWithYearName(id: Int): TermResponseDto? = transaction {

        TermTable
            .join(
                AcademicYearTable,
                JoinType.INNER,
                onColumn = TermTable.academic_year,
                otherColumn = AcademicYearTable.id
            )
            .selectAll()
            .where { TermTable.id eq id }
            .singleOrNull()
            ?.let { row ->

                val academicYear = AcademicYearResponse(
                    id = row[AcademicYearTable.id].value,
                    name = row[AcademicYearTable.name]
                )

                TermResponseDto(
                    id = row[TermTable.id].value,
                    name = row[TermTable.name],
                    academic_year = academicYear
                )
            }
    }

    fun findAllWithYearName(): List<TermResponseDto> = transaction {

        val query = TermTable
            .join(AcademicYearTable, JoinType.INNER, onColumn = TermTable.academic_year, otherColumn = AcademicYearTable.id)

        query
            .selectAll()
            .orderBy(TermTable.id, SortOrder.DESC)
            .map {
                row ->
                val academicYear = AcademicYearResponse(
                    id = row[AcademicYearTable.id].value,
                    name = row[AcademicYearTable.name],

                )

                TermResponseDto(
                    id = row[TermTable.id].value,
                    name = row[TermTable.name],
                    academic_year = academicYear
                )



            }



    }

    fun delete(id: Int) = transaction {
        TermTable.deleteWhere { TermTable.id eq id }
    } > 0



    fun findById(id: Int): TermModel? = transaction {
        TermTable
            .selectAll()
            .where{ TermTable.id eq id }
            .singleOrNull()
            ?.toTermModel()
    }

    fun patch(id: Int, req: PatchTermRequest): TermModel? = transaction {

        val rowsUpdated = TermTable.update(
            where = { TermTable.id eq id }
        ) { row ->

            req.name?.let {
                row[TermTable.name] = it
            }

            req.academic_year_id?.let {
                row[TermTable.academic_year] = EntityID(it, AcademicYearTable)
            }
        }

        if (rowsUpdated == 0) {
            null
        } else {
            findById(id)
        }
    }



}