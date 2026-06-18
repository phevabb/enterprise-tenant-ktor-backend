package com.example.admin.mappers




import com.example.admin.models.AdminProfile
import com.example.admin.tables.AdminTable

import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toAdminProfile() = AdminProfile(
    id = this[AdminTable.id].value,
    user = this[AdminTable.user]?.value,
    tel = this[AdminTable.tel]
)
