package com.example.student.repos

import com.example.account.AccountTable
import com.example.student.StudentsTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction


import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

object StudentLiteRepo {
    fun getStudentClassLevelId(studentId: Int): Int? = transaction {
        StudentsTable.selectAll()
            .where { StudentsTable.id eq studentId }
            .singleOrNull()
            ?.get(StudentsTable.currentNewGradeClass)
            ?.value
    }


    fun getStudentProfileIdByAccountId(accountId: Int): Int? = transaction {
        StudentsTable
            .selectAll()
            .where { StudentsTable.user eq EntityID(accountId, AccountTable) }
            .singleOrNull()
            ?.get(StudentsTable.id)
            ?.value
    }
}



