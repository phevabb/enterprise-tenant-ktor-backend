package com.example.familyfees.dtos.responses

import com.example.familyfees.tables.FamilyTable
import com.example.fees.tables.PaymentTable
import org.jetbrains.exposed.sql.ResultRow


fun ResultRow.toFamilyResponseDto(): FamilyResponseDto = FamilyResponseDto(
    id = this[FamilyTable.id].value,
    name = this[FamilyTable.name],
)