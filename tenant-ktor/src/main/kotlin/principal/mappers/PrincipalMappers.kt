package com.example.principal.mappers
import com.example.principal.tables.PrincipalTable


import org.jetbrains.exposed.sql.ResultRow
import com.example.principal.models.PrincipalProfile

fun ResultRow.toPrincipalProfile() = PrincipalProfile(
    id = this[PrincipalTable.id].value,
    user = this[PrincipalTable.user]?.value
)