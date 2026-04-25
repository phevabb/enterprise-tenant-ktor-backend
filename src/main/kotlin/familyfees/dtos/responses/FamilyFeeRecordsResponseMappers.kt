package com.example.familyfees.dtos.responses

import com.example.familyfees.tables.FamilyFeeRecordTable
import com.example.familyfees.tables.FamilyTable
import com.example.minimals.AcademicYearMinimal
import com.example.minimals.FamilyMinimal
import com.example.minimals.TermMinimal
import com.example.student.tables.AcademicYearTable
import com.example.student.tables.TermTable
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toFamilyFeeRecordsResponseDto() = FamilyFeeRecordsResponseDto(

    id = this[FamilyFeeRecordTable.id].value,

    family = FamilyMinimal(
        id = this[FamilyTable.id].value,
        name = this[FamilyTable.name],
    ),
    term = TermMinimal(
        name = this[TermTable.name],
    ),
    academic_year = AcademicYearMinimal(
        name = this[AcademicYearTable.name],
    ),
    amount_to_pay = this[FamilyFeeRecordTable.amount_to_pay],
    amount_paid = this[FamilyFeeRecordTable.amount_paid],
    balance = this[FamilyFeeRecordTable.balance],
    is_fully_paid = this[FamilyFeeRecordTable.is_fully_paid],

    date_created = this[FamilyFeeRecordTable.date_created],
)
