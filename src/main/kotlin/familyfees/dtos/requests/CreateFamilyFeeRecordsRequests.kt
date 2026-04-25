package com.example.familyfees.dtos.requests

import kotlinx.serialization.Serializable


@Serializable
data class CreateFamilyFeeRecordsRequests(
    val family : Int,
    val amount_to_pay: Int,
    val amount_paid: Int,
    val term: Int,
    val academic_year: Int,

    )
