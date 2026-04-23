package com.example.fees.models

import kotlinx.serialization.Serializable

@Serializable
data class StudentFeeRecordModel(
    val id: Int,
    val studentId: Int,
    val feeStructureId: Int,
    val amountPaid: Int,
    val balance: Int,
    val isFullyPaid: Boolean,
    val dateCreated: Long // epoch millis
)
