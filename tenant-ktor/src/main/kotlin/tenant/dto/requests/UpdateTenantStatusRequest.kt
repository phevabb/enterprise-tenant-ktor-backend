package com.example.tenant.dto.requests



import kotlinx.serialization.Serializable

@Serializable
data class UpdateTenantStatusRequest(
    val status: String
)