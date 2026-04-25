package com.example.familyfees.mappers

import com.example.familyfees.models.FamilyFeeModel
import com.example.familyfees.tables.FamilyFeeRecordTable
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toFamilyFeeModel()= FamilyFeeModel (
    id = this[FamilyFeeRecordTable.id].value,
    family = this[FamilyFeeRecordTable.family].value,
    amount_to_pay = this[FamilyFeeRecordTable.amount_to_pay],
    amount_paid = this[FamilyFeeRecordTable.amount_paid],
    balance = this[FamilyFeeRecordTable.balance],
    is_fully_paid = this[FamilyFeeRecordTable.is_fully_paid],
    term = this[FamilyFeeRecordTable.term].value,
    academic_year = this[FamilyFeeRecordTable.academic_year].value,
    date_created = this[FamilyFeeRecordTable.date_created],

)