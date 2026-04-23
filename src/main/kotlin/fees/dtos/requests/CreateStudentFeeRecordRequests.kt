package com.example.fees.dtos.requests


import kotlinx.serialization.Serializable

@Serializable
data class CreateStudentFeeRecordRequest(
    val studentId: Int,
    val feeStructureId: Int
)

@Serializable
data class AddPaymentRequest(
    val payment: Int
)