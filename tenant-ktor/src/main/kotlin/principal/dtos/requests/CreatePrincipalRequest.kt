package com.example.principal.dtos.requests



import kotlinx.serialization.Serializable

@Serializable
data class CreatePrincipalRequest(
    val user: CreateUserPart
)

@Serializable
data class CreateUserPart(
    val fullName: String,
    val gender: String? = null,
    val dateOfBirth: String? = null,
    val nationality: String? = null,
    val role: String = "principal",
    val isActive: Boolean = true,
    val isStaff: Boolean = true
)
