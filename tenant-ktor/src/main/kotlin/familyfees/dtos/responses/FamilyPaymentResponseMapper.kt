package com.example.familyfees.dtos.responses

import com.example.familyfees.tables.FamilyFeeRecordTable
import com.example.familyfees.tables.FamilyPaymentTable
import com.example.familyfees.tables.FamilyTable
import com.example.minimals.AcademicYearMinimal
import com.example.minimals.FamilyFeeRecordMinimal
import com.example.minimals.FamilyMinimal
import com.example.minimals.TermMinimal
import com.example.student.tables.AcademicYearTable
import com.example.student.tables.TermTable
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toFamilyPaymentResponseDto(): FamilyPaymentResponseDto {

    val familyMinimal = this[FamilyTable.id]?.value?.let { famId ->
        FamilyMinimal(
            id = famId,
            name = this[FamilyTable.name]
        )
    }

    val termMinimal = this[TermTable.id]?.value?.let {
        TermMinimal(
            name = this[TermTable.name]
        )
    }

    val academicYearMinimal = this[AcademicYearTable.id]?.value?.let {
        AcademicYearMinimal(
            name = this[AcademicYearTable.name]
        )
    }

    val feeRecord = FamilyFeeRecordMinimal(
        id = this[FamilyFeeRecordTable.id].value,
        family = familyMinimal,
        term = termMinimal,
        academic_year = academicYearMinimal,
        amount_to_pay = this[FamilyFeeRecordTable.amount_to_pay],
        amount_paid = this[FamilyFeeRecordTable.amount_paid],
        balance = this[FamilyFeeRecordTable.balance],
        is_fully_paid = this[FamilyFeeRecordTable.is_fully_paid],
        date_created = this[FamilyFeeRecordTable.date_created] // ✅ fee record created date (keep)
    )

    return FamilyPaymentResponseDto(
        id = this[FamilyPaymentTable.id].value,
        family_fee_record = feeRecord,
        amount = this[FamilyPaymentTable.amount],
        date_created = this[FamilyPaymentTable.date_created], // ✅ FIX: payment created date
        balance = this[FamilyFeeRecordTable.balance],
    )
}