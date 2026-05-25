package com.example.academics.repos

import com.example.academics.tables.GradesTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object GradeRepoLite {
    fun findGradeByScore(total: Int): Triple<Int, String, String>? = transaction {
        val row = GradesTable.selectAll()
            .where {
                (GradesTable.minScore lessEq total) and
                        (GradesTable.maxScore greaterEq total)
            }
            .orderBy(GradesTable.order to SortOrder.ASC)
            .limit(1)
            .singleOrNull()

        row?.let {
            Triple(
                it[GradesTable.id].value,
                it[GradesTable.code],
                it[GradesTable.label]
            )
        }
    }
}