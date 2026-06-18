package com.example.minimals

import kotlinx.serialization.Serializable

@Serializable
data class AccountNameOnly(
    val fullName: String
)