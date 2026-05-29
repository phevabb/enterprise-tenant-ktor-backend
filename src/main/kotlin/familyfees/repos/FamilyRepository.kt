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

    fun findById(id: Int): FamilyResponseDto? = transaction {

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
            .where{ FamilyTable.id eq id }
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
        name:String,
    ) = transaction {
        val id = FamilyTable.insertAndGetId {
            it[FamilyTable.name] = name

        }.value
        findById(id)?:error("Family not found")
    }

    fun findAll(): List<FamilyResponseDto> = transaction {

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
        page: Int,
        limit: Int,
        search: String?
    ): Pair<List<FamilyResponseDto>, Long> = transaction {

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

        // ✅ SIMPLE SEARCH (case-insensitive using contains ignoreCase)
        val filtered = if (!search.isNullOrBlank()) {
            rows.filter { r ->
                val familyName = r[FamilyTable.name]
                val studentName = r.getOrNull(AccountTable.fullName) ?: ""

                familyName.contains(search, ignoreCase = true) ||
                        studentName.contains(search, ignoreCase = true)
            }
        } else rows

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

        return@transaction Pair(paginated, total)
    }

    fun delete(id: Int): Boolean = transaction {
        FamilyTable.deleteWhere { FamilyTable.id eq id } > 0
    }

    fun update(id: Int, name: String): FamilyResponseDto? = transaction {

        val exists = FamilyTable
            .selectAll()
            .where { FamilyTable.id eq id }
            .singleOrNull() ?: return@transaction null

        FamilyTable.update({ FamilyTable.id eq id }) {
            it[FamilyTable.name] = name
        }

        // return updated family (reuse your mapping)
        findById(id)
    }




}










