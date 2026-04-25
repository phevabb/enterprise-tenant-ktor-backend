package com.example.familyfees.models

import kotlinx.serialization.Serializable

@Serializable
data class FamilyFeeModel(
    val id: Int,
    val family : Int,
    val amount_to_pay: Int,
    val amount_paid: Int,
    val balance: Int,
    val is_fully_paid: Boolean,
    val term: Int,
    val academic_year: Int,
    val date_created: Long,
)
