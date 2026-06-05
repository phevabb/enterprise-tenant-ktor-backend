package com.example.tenant.dto.response


import kotlinx.serialization.Serializable

@Serializable
data class CreateTenantResponse(
    val tenantId: Int,
    val schoolName: String,
    val tenantCode: String,
    val tenantSchema: String,
    val tenantSlug: String,
    val defaultDomain: String,
    val status: String,
    val message: String,

    // Bootstrap principal login info
    val principalLoginUserId: String,
    val principalPin: String
)