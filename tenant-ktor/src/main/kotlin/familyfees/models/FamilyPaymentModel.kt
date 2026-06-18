package com.example.familyfees.models

import kotlinx.serialization.Serializable

@Serializable
data class FamilyPaymentModel(
    val id: Int,
    val family_fee_record: Int,
    val payment_method: String?,
    val amount: Int,
    val date_created: Long,
)
