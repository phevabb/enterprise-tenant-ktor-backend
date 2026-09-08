package com.example.account

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import others.LocalDateSerializer
import java.time.LocalTime
import java.time.Instant
import java.time.LocalDate

@Serializable
data class Account(
    val id: Int,
    val userId: String,
    @Serializable(with = LocalDateSerializer::class)
    val dateOfBirth: LocalDate? = null,
    val role: Role,
    val pin: String? = null,
    val fullName: String,
    val isActive: Boolean = true,
    val isStaff: Boolean=false,
    val gender: Gender? = null,


    @Contextual val createdAt: Instant? = null,

    val nationality: String? = null,
    val email: String? = null,
    val profilePictureUrl: String? = null,


    )