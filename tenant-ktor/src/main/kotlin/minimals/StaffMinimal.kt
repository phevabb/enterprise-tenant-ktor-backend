package com.example.minimals

import kotlinx.serialization.Serializable


@Serializable
data class StaffUserMinimal(
    val id: Int,
    val pin: Int,
    val userId: String,
    val fullName: String,
    val gender: String?,
    val role: String,
    val isActive: Boolean,
    val dateOfBirth: String?
)