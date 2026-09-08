package com.example.minimals

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import others.LocalDateSerializer
import java.time.LocalDate


@Serializable
data class StaffUserMinimal(
    val id: Int,
    val pin: Int,
    val userId: String,
    val fullName: String,
    val gender: String?,
    val role: String,
    val isActive: Boolean,
    @Serializable(with = LocalDateSerializer::class)
    val dateOfBirth: LocalDate? = null,
)