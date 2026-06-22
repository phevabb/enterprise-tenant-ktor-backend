package com.example.tenant.dto.response


import kotlinx.serialization.Serializable

@Serializable
data class BootstrapAdminResponse(
    val accountId: Int,
    val loginUserId: String,
    val pin: String
)