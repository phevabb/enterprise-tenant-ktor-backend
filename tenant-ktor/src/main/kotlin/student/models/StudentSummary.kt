package com.example.student.models

import com.example.account.Account
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class StudentSummary(
    val id: Int,
    @Contextual
    val user: Account
)