package com.example.superadmin.dto.response



import kotlinx.serialization.Serializable

@Serializable
data class SuperAdminTransactionResponse(
    val id: Int,
    val accountId: Int,
    val tenantCode: String,
    val academicYearId: Int?,
    val academicTermId: Int?,
    val studentCount: Int,
    val amountPerStudentCedis: Double,
    val totalAmountCedis: Double,
    val isPaid: Boolean,
    val paymentStatus: String,
    val paystackReference: String?,
    val dueDateEpochMillis: Long?,
    val paidAtEpochMillis: Long?,
    val createdAtEpochMillis: Long?
)