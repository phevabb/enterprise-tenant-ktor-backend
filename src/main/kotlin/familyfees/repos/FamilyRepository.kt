package com.example.familyfees.repos

import com.example.account.AccountTable
import com.example.familyfees.dtos.responses.FamilyReceiptDto
import com.example.familyfees.dtos.responses.FamilyResponseDto
import com.example.familyfees.tables.FamilyReceiptsTable

import com.example.familyfees.tables.FamilyTable
import com.example.fees.tables.FeeStructureTable
import com.example.minimals.StudentMinimalDto
import com.example.student.StudentsTable
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.countDistinct
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.random.Random

object FamilyRepository {

    private fun Transaction.setTenantSchema(tenantSchema: String) {
        val safeSchema = tenantSchema.replace("\"", "\"\"")
        exec("""SET LOCAL search_path TO "$safeSchema"""")
    }


    fun findById(
        tenantSchema: String,
        id: Int
    ): FamilyResponseDto? = transaction {

        setTenantSchema(tenantSchema)

        val rows = FamilyTable
            .join(
                StudentsTable,
                JoinType.LEFT,
                additionalConstraint = { StudentsTable.family eq FamilyTable.id }
            )
            .join(
                AccountTable,
                JoinType.LEFT,
                onColumn = StudentsTable.user,
                otherColumn = AccountTable.id
            )
            .selectAll()
            .where { FamilyTable.id eq id }
            .toList()

        if (rows.isEmpty()) return@transaction null

        val first = rows.first()

        val members = rows.mapNotNull { r ->
            r[StudentsTable.id]?.value?.let { studentId ->
                StudentMinimalDto(
                    id = studentId,
                    name = r[AccountTable.fullName]
                )
            }
        }

        FamilyResponseDto(
            id = first[FamilyTable.id].value,
            name = first[FamilyTable.name],
            is_active = first[FamilyTable.is_active],
            members = members
        )
    }


    fun create(
        tenantSchema: String,
        name: String
    ) = transaction {

        setTenantSchema(tenantSchema)

        val id = FamilyTable.insertAndGetId {
            it[FamilyTable.name] = name
        }.value

        findById(tenantSchema, id)
            ?: error("Family not found")
    }

    fun findAll(
        tenantSchema: String
    ): List<FamilyResponseDto> = transaction {

        setTenantSchema(tenantSchema)

        val rows = FamilyTable
            .join(
                StudentsTable,
                JoinType.LEFT,
                additionalConstraint = { StudentsTable.family eq FamilyTable.id }
            )
            .join(
                AccountTable,
                JoinType.LEFT,
                onColumn = StudentsTable.user,
                otherColumn = AccountTable.id
            )
            .selectAll()
            .orderBy(FamilyTable.id to SortOrder.DESC)
            .toList()

        rows
            .groupBy { it[FamilyTable.id].value }
            .map { (_, familyRows) ->

                val first = familyRows.first()

                val members = familyRows.mapNotNull { r ->
                    r[StudentsTable.id]?.value?.let { studentId ->
                        StudentMinimalDto(
                            id = studentId,
                            name = r[AccountTable.fullName]
                        )
                    }
                }

                FamilyResponseDto(
                    id = first[FamilyTable.id].value,
                    name = first[FamilyTable.name],
                    is_active = first[FamilyTable.is_active],
                    members = members
                )
            }
    }

    fun findAllPaginated(
        tenantSchema: String,
        page: Int,
        limit: Int,
        search: String?
    ): Pair<List<FamilyResponseDto>, Long> = transaction {

        setTenantSchema(tenantSchema)

        val offset = ((page - 1) * limit).toLong()

        val rows = FamilyTable
            .join(
                StudentsTable,
                JoinType.LEFT,
                additionalConstraint = { StudentsTable.family eq FamilyTable.id }
            )
            .join(
                AccountTable,
                JoinType.LEFT,
                onColumn = StudentsTable.user,
                otherColumn = AccountTable.id
            )
            .selectAll()
            .orderBy(FamilyTable.id, SortOrder.DESC)
            .toList()

        val filtered = if (!search.isNullOrBlank()) {
            rows.filter { r ->
                val familyName = r[FamilyTable.name]
                val studentName = r.getOrNull(AccountTable.fullName) ?: ""

                familyName.contains(search, ignoreCase = true) ||
                        studentName.contains(search, ignoreCase = true)
            }
        } else {
            rows
        }

        val grouped = filtered.groupBy { it[FamilyTable.id].value }

        val allFamilies = grouped.map { (_, familyRows) ->

            val first = familyRows.first()

            val members = familyRows.mapNotNull { r ->
                r.getOrNull(StudentsTable.id)?.value?.let {
                    StudentMinimalDto(
                        id = it,
                        name = r.getOrNull(AccountTable.fullName) ?: ""
                    )
                }
            }.distinctBy { it.id }

            FamilyResponseDto(
                id = first[FamilyTable.id].value,
                name = first[FamilyTable.name],
                is_active = first[FamilyTable.is_active],
                members = members
            )
        }

        val total = allFamilies.size.toLong()

        val paginated = allFamilies
            .drop(offset.toInt())
            .take(limit)

        Pair(paginated, total)
    }

    fun delete(
        tenantSchema: String,
        id: Int
    ): Boolean = transaction {

        setTenantSchema(tenantSchema)

        FamilyTable.deleteWhere {
            FamilyTable.id eq id
        } > 0
    }

    fun update(
        tenantSchema: String,
        id: Int,
        name: String
    ): FamilyResponseDto? = transaction {

        setTenantSchema(tenantSchema)

        val exists = FamilyTable
            .selectAll()
            .where { FamilyTable.id eq id }
            .singleOrNull()
            ?: return@transaction null

        FamilyTable.update({ FamilyTable.id eq id }) {
            it[FamilyTable.name] = name
        }

        findById(tenantSchema, id)
    }




}










