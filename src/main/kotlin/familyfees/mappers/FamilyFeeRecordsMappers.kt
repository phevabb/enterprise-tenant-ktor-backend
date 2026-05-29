package com.example.familyfees.mappers

import com.example.familyfees.models.FamilyFeeRecordModel
import com.example.familyfees.tables.FamilyFeeRecordTable
import com.example.minimals.AcademicYearMinimal
import com.example.minimals.FamilyFeeRecordMinimal
import com.example.minimals.FamilyMinimal
import com.example.minimals.TermMinimal
import org.jetbrains.exposed.sql.ResultRow
fun ResultRow.toFamilyFeeRecordMinimalAfterUpdate(
    amountPaid: Int,
    newBalance: Int,
    isFullyPaid: Boolean,
    familyMinimal: FamilyMinimal? = null,
    termMinimal: TermMinimal? = null,
    academicYearMinimal: AcademicYearMinimal? = null
): FamilyFeeRecordMinimal {
    return FamilyFeeRecordMinimal(
        id = this[FamilyFeeRecordTable.id].value,
        family = familyMinimal,
        term = termMinimal,
        academic_year = academicYearMinimal,
        amount_to_pay = this[FamilyFeeRecordTable.amount_to_pay],
        amount_paid = amountPaid,
        balance = newBalance,
        is_fully_paid = isFullyPaid,
        date_created = this[FamilyFeeRecordTable.date_created]
    )
}