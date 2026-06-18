package com.example.fees.dtos.responses



import kotlinx.serialization.Serializable

@Serializable
data class ReceiptResponse(
    val id: Int,
    val receiptNo: String,
    val paymentId: Int,
    val studentFeeRecordId: Int,
    val studentId: Int,
    val studentName: String,
    val amountPaid: Int,
    val balanceAfter: Int,
    val paymentMethod: String? = null,
    val className: String,
    val termName: String,
    val academicYearName: String,
    val createdAt: Long,
    val receiptUrl: String? = null
)