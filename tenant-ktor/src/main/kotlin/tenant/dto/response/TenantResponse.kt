package com.example.tenant.dto.response

import kotlinx.serialization.Serializable


@Serializable
data class TenantResponse(
    val schoolName: String,
    val tenantCode: String,
    val tenantSlug: String
)
