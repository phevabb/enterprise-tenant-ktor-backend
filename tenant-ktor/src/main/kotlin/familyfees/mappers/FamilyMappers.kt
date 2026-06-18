package com.example.familyfees.mappers
import com.example.familyfees.tables.FamilyTable
import com.example.familyfees.models.FamilyModel
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toFamilyModel() = FamilyModel (
    id = this[FamilyTable.id].value,
    name = this[FamilyTable.name],
    is_active = this[FamilyTable.is_active]


    )