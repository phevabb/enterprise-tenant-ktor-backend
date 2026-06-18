package com.example.notifications



import kotlinx.serialization.Serializable

@Serializable
data class StudentFeeRecordSnapshotDto(
    val id: Int,
    val studentId: Int,
    val feeStructureId: Int,
    val amountPaid: Int,
    val balance: Int,
    val isFullyPaid: Boolean,
    val dateCreated: Long
)