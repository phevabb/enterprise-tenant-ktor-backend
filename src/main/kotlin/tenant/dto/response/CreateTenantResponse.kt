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
    val defaultLocalDomain: String,
    val fallbackLocalUrl: String,
    val status: String,
    val message: String,
    val principalLoginUserId: String,
    val principalPin: String
)