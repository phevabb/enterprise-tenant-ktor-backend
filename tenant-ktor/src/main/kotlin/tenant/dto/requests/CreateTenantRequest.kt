package com.example.tenant.dto.requests

import kotlinx.serialization.Serializable

@Serializable
data class CreateTenantRequest(
    val schoolName: String,
    val tenantCode: String,
    val schoolType: String? = null,
    val location: String? = null,
    val contactEmail: String? = null,
    val accountOwnerName: String? = null,
    val primaryDomain: String? = null,
    val academicYear: String? = null,
    val features: List<String> = listOf("students", "fees", "staff", "academics")
)