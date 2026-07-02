package com.example.tenant.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class InternalTenantBillingSummaryItem(
    val tenantId: Int,
    val schoolName: String,
    val tenantCode: String,
    val location: String?,
    val academicYear: String?,
    val status: String,
    val studentCount: Int
)