package com.example.staff.repos

import com.example.academics.dtos.response.AssignedClassDto
import com.example.academics.dtos.response.AssignedClassResponse
import com.example.academics.repos.setTenantSchema


import com.example.staff.tables.StaffTable
import com.example.student.tables.NewGradeClassTable
import org.jetbrains.exposed.sql.*

import com.example.account.AccountTable
import org.jetbrains.exposed.sql.transactions.transaction

object StaffAssignedClassRepository {

    fun findAssignedClassByUserId(
        tenantSchema: String,
        userId: String
    ): AssignedClassResponse? = transaction {

        setTenantSchema(tenantSchema)

        val row = StaffTable
            .join(AccountTable, JoinType.INNER, StaffTable.user, AccountTable.id)
            .join(NewGradeClassTable, JoinType.LEFT, StaffTable.assignedClass, NewGradeClassTable.id)
            .selectAll()
            .where { AccountTable.userId eq userId }
            .singleOrNull()
            ?: return@transaction null

        val assigned = row.getOrNull(NewGradeClassTable.id)?.value?.let { classId ->
            AssignedClassDto(
                id = classId,
                name = row[NewGradeClassTable.name]
            )
        }

        AssignedClassResponse(
            userId = userId,
            assignedClass = assigned
        )
    }}

