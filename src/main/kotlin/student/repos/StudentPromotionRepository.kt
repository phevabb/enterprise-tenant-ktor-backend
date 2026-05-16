package com.example.student.repos


import com.example.student.tables.NewClassPromotionTable
import com.example.student.StudentsTable

import com.example.account.AccountTable
import org.jetbrains.exposed.sql.Case

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

import com.example.student.tables.*
import org.jetbrains.exposed.dao.id.EntityID


object StudentPromotionRepository {

    fun promoteAllStudentsForNewAcademicYear() = transaction {

        // Load promotion rules: currentStageId -> nextStageId (nullable = graduate)
        val rules: Map<Int, Int?> = NewClassPromotionTable
            .selectAll()
            .associate { row ->
                row[NewClassPromotionTable.currentStageId].value to
                        row[NewClassPromotionTable.nextStageId]?.value
            }

        // Load all active/non-graduated students FIRST (snapshot)
        val students = StudentsTable
            .selectAll()
            .where { StudentsTable.isGraduated eq false }
            .mapNotNull {
                val currentStageId = it[StudentsTable.currentNewGradeClass]?.value
                    ?: return@mapNotNull null // ✅ skip students with no current class

                Triple(
                    it[StudentsTable.id].value,
                    currentStageId,
                    it[StudentsTable.user].value
                )
            }

        val accountsToDeactivate = mutableListOf<Int>()

        students.forEach { (studentId, currentStageId, accountId) ->

            // ✅ Django behavior: if no rule exists, skip
            if (!rules.containsKey(currentStageId)) return@forEach

            val nextStageId = rules[currentStageId]

            if (nextStageId != null) {
                // Promote student once
                StudentsTable.update({
                    StudentsTable.id eq EntityID(studentId, StudentsTable)
                }) {
                    it[currentNewGradeClass] = EntityID(nextStageId, NewGradeClassTable)
                }

            } else {
                // Graduate student (final class)
                StudentsTable.update({
                    StudentsTable.id eq EntityID(studentId, StudentsTable)
                }) {
                    it[isGraduated] = true
                }

                accountsToDeactivate.add(accountId)
            }
        }

        // Deactivate graduated student accounts
        accountsToDeactivate.distinct().chunked(500).forEach { chunk ->
            AccountTable.update({ AccountTable.id inList chunk }) {
                it[isActive] = false
            }
        }
    }
}





