package com.example.familyfees.dtos.responses



import kotlinx.serialization.Serializable

@Serializable
data class FamilyReceiptDto(
    val id: Int,
    val receiptNo: String,
    val familyPaymentId: Int,
    val familyFeeRecordId: Int,

    val familyName: String,
    val wards: List<String>,

    val amountPaid: Int,
    val balanceAfter: Int,
    val paymentMethod: String,

    val termName: String?,
    val academicYearName: String?,

    val createdAt: Long,

    // ✅ frontend can call this to download
    val pdfUrl: String
)
