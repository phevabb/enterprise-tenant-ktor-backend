package com.example.fees.dtos.requests

import kotlinx.serialization.Serializable

@Serializable
data class CreatePaymentRequest(
    val student_fee_record_id: Int,
    val amount: Int
)
//student_fee_record_id: formPayment.studentFeeRecordId,
//date: formPayment.date || today,
//amount: formPayment.amount