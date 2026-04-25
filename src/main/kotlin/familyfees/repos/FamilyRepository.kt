package com.example.familyfees.repos

import com.example.familyfees.dtos.responses.FamilyResponseDto
import com.example.familyfees.dtos.responses.toFamilyResponseDto
import com.example.familyfees.tables.FamilyTable
import com.example.fees.tables.FeeStructureTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object FamilyRepository {

    fun findById(id:Int): FamilyResponseDto? = transaction {
        FamilyTable
            .selectAll()
            .where{ FamilyTable.id eq id }
            .singleOrNull()
            ?.toFamilyResponseDto()


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
        FamilyTable
            .selectAll()
            .orderBy(FamilyTable.id, SortOrder.DESC)
            .map { it.toFamilyResponseDto() }
    }

    fun delete(id: Int): Boolean = transaction {
        FamilyTable.deleteWhere { FamilyTable.id eq id } > 0
    }


}