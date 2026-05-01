package com.example.familyfees.repos

import com.example.account.AccountTable
import com.example.familyfees.dtos.responses.FamilyResponseDto

import com.example.familyfees.tables.FamilyTable
import com.example.fees.tables.FeeStructureTable
import com.example.minimals.StudentMinimalDto
import com.example.student.StudentsTable
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

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


    fun delete(id: Int): Boolean = transaction {
        FamilyTable.deleteWhere { FamilyTable.id eq id } > 0
    }


}