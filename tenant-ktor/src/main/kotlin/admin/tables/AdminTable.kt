package com.example.admin.tables

import com.example.account.AccountTable
import com.example.student.tables.NewGradeClassTable
import org.jetbrains.exposed.dao.id.IntIdTable

object AdminTable : IntIdTable("admin_profile") {
    val user = reference("user", AccountTable)
        .uniqueIndex()
        .nullable() // because on_delete = SET_NULL
    val tel = varchar("tel", 30).nullable()
}