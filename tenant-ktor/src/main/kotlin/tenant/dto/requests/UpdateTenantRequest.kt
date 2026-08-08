package com.example.tenant.dto.requests

import kotlinx.serialization.Serializable

@Serializable
data class UpdateTenantRequest(
    val tenantCode: String,
    val schoolLogoUrl: String?,

    val schoolName: String,

    val schoolMotto: String?,

    val schoolLocation: String?,

    val fullName: String?,

    val phoneNumber: String?,

    val adminPin: String?,

    val principalPin: String?
)