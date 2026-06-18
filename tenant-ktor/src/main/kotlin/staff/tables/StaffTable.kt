package com.example.staff.tables

import com.example.account.AccountTable
import com.example.student.tables.NewGradeClassTable
import org.jetbrains.exposed.dao.id.IntIdTable

object StaffTable : IntIdTable("staff_profile") {

    /**
     * Django: OneToOneField(User)
     * SQL: UNIQUE foreign key to accounts(id)
     */
    val user = reference("user", AccountTable)
        .uniqueIndex()
        .nullable() // because on_delete = SET_NULL

    /**
     * Django: ForeignKey(NewGradeClass, null=True, blank=True)
     */
    val assignedClass = reference(
        "assigned_class_id",
        NewGradeClassTable
    ).nullable()

    /**
     * Django: CharField(max_length=30, null=True, blank=True)
     */
    val tel = varchar("tel", 30).nullable()
}