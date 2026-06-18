package com.example.staff.mappers


import com.example.staff.tables.StaffTable
import com.example.staff.models.StaffProfile
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toStaffProfile() = StaffProfile(
    id = this[StaffTable.id].value,

    user = this[StaffTable.user]?.value,

    assignedClassId = this[StaffTable.assignedClass]?.value,

    tel = this[StaffTable.tel]
)
