package com.example.tenant.dto.response



import kotlinx.serialization.Serializable

@Serializable
data class UpdateTenantStatusResponse(
    val message: String,
    val tenantCode: String,
    val status: String
)