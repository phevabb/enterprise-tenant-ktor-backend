package com.example.minimals

import kotlinx.serialization.Serializable

@Serializable
data class StudentNameOnly(
    val id: Int,
    val user: AccountNameOnly
)
