package com.example.minimals

import kotlinx.serialization.Serializable

@Serializable
data class FamilyFeeRecordMinimal(
    val id: Int,
    val family: FamilyMinimal?,
    val term: TermMinimal?,
    val academic_year: AcademicYearMinimal?,
    val amount_to_pay: Int,
    val amount_paid: Int,
    val balance: Int,
    val is_fully_paid: Boolean,
    val date_created: Long,




    )
