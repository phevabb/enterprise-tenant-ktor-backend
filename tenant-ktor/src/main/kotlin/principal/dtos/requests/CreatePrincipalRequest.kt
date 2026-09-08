package com.example.principal.dtos.requests



import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import others.LocalDateSerializer
import java.time.LocalDate

@Serializable
data class CreatePrincipalRequest(
    val user: CreateUserPart
)

@Serializable
data class CreateUserPart(
    val fullName: String,
    val gender: String? = null,
    @Serializable(with = LocalDateSerializer::class)
    val dateOfBirth: LocalDate? = null,
    val nationality: String? = null,
    val role: String = "principal",
    val isActive: Boolean = true,
    val isStaff: Boolean = true
)
