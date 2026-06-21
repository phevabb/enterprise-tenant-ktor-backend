package com.example.tenant.dto



import kotlinx.serialization.Serializable

@Serializable
data class SuperAdminTenantResponse(
    val id: Int,
    val schoolName: String,
    val tenantCode: String,
    val tenantSchema: String,
    val tenantSlug: String,
    val defaultDomain: String,

    val schoolType: String?,
    val location: String?,
    val contactEmail: String?,
    val accountOwnerName: String?,
    val primaryDomain: String?,

    val academicYear: String?,
    val status: String,
    val createdAt: String,

    val schemaName: String,
    val schemaStatus: String
)

@Serializable
data class UpdateTenantStatusRequest(
    val status: String
)

@Serializable
data class TenantStatusUpdateResponse(
    val success: Boolean,
    val message: String
)