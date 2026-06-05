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
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.update




fun Transaction.setTenantSchema(tenantSchema: String) {
    val safeSchema = tenantSchema.replace("\"", "\"\"")
    exec("""SET LOCAL search_path TO "$safeSchema"""")
}


object TermRepository {

    fun getCurrent(tenantSchema: String): Pair<Int, Int>? = transaction {

        setTenantSchema(tenantSchema)

        val row = TermTable
            .selectAll()
            .orderBy(TermTable.id, SortOrder.DESC)
            .limit(1)
            .singleOrNull()
            ?: return@transaction null

        val termId = row[TermTable.id].value
        val yearId = row[TermTable.academic_year].value

        termId to yearId
    }

    fun findAll(tenantSchema: String): List<TermModel> = transaction {

        setTenantSchema(tenantSchema)

        TermTable
            .selectAll()
            .orderBy(TermTable.id, SortOrder.DESC)
            .map { it.toTermModel() }
    }

    fun create(
        tenantSchema: String,
        term: TermModel
    ): TermResponseDto = transaction {

        setTenantSchema(tenantSchema)

        val id = TermTable.insertAndGetId {
            it[name] = term.name
            it[academic_year] = term.academic_year
        }.value


        findByIdWithYearName(tenantSchema, id)
            ?: error("Term created but not found")
    }

    fun findByIdWithYearName(
        tenantSchema: String,
        id: Int
    ): TermResponseDto? = transaction {

        setTenantSchema(tenantSchema)

        TermTable
            .join(
                AcademicYearTable,
                JoinType.INNER,
                TermTable.academic_year,
                AcademicYearTable.id
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

    fun findAllWithYearName(
        tenantSchema: String
    ): List<TermResponseDto> = transaction {

        setTenantSchema(tenantSchema)

        TermTable
            .join(
                AcademicYearTable,
                JoinType.INNER,
                TermTable.academic_year,
                AcademicYearTable.id
            )
            .selectAll()
            .orderBy(TermTable.id, SortOrder.DESC)
            .map { row ->

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

    fun delete(
        tenantSchema: String,
        id: Int
    ): Boolean = transaction {

        setTenantSchema(tenantSchema)

        TermTable.deleteWhere { TermTable.id eq id } > 0
    }

    fun findById(
        tenantSchema: String,
        id: Int
    ): TermModel? = transaction {

        setTenantSchema(tenantSchema)

        TermTable
            .selectAll()
            .where { TermTable.id eq id }
            .singleOrNull()
            ?.toTermModel()
    }

    fun patch(
        tenantSchema: String,
        id: Int,
        req: PatchTermRequest
    ): TermModel? = transaction {

        setTenantSchema(tenantSchema)

        val rowsUpdated = TermTable.update(
            where = { TermTable.id eq id }
        ) { row ->

            req.name?.let {
                row[TermTable.name] = it
            }

            req.academic_year_id?.let {
                row[TermTable.academic_year] =
                    EntityID(it, AcademicYearTable)
            }
        }

        if (rowsUpdated == 0) null else findById(tenantSchema, id)
    }

    fun getCurrentt(): TermResponseDto? {

        val row = TermTable
            .join(
                AcademicYearTable,
                JoinType.INNER,
                TermTable.academic_year,
                AcademicYearTable.id
            )
            .selectAll()
            .orderBy(TermTable.id, SortOrder.DESC)
            .limit(1)
            .singleOrNull()
            ?: return null

        return TermResponseDto(
            id = row[TermTable.id].value,
            name = row[TermTable.name],
            academic_year = AcademicYearResponse(
                id = row[AcademicYearTable.id].value,
                name = row[AcademicYearTable.name]
            )
        )
    }


}




