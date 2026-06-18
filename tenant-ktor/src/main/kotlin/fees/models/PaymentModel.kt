package com.example.fees.models

import kotlinx.serialization.Serializable

@Serializable
data class PaymentModel(
    val id: Int,
    val student_fee_record: Int,
    val payment_method: String?,
    val amount: Int,
    val date_created: Long,
)
