package com.example.student.repos

import com.example.student.StudentsTable
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
}