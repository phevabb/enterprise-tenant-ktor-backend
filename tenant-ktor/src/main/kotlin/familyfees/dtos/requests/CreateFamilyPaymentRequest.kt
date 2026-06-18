package com.example.familyfees.dtos.requests

import kotlinx.serialization.Serializable

@Serializable
data class CreateFamilyPaymentRequest(
    val family_fee_record: Int,
    val amount: Int,
)
