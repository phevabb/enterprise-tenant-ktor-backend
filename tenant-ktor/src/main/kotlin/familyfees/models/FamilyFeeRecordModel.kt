package com.example.familyfees.models

import com.example.minimals.AcademicYearMinimal
import com.example.minimals.FamilyMinimal
import com.example.minimals.TermMinimal
import kotlinx.serialization.Serializable

@Serializable
data class FamilyFeeRecordModel(
    val id: Int,
    val family : FamilyMinimal,
    val amount_to_pay: Int,
    val amount_paid: Int,
    val balance: Int,
    val is_fully_paid: Boolean,
    val term: TermMinimal,
    val academic_year: AcademicYearMinimal,
    val date_created: Long,
)
