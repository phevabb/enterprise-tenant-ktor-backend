package com.example.principal.dtos.responses



import kotlinx.serialization.Serializable

@Serializable
data class CreatePrincipalResponse(
    val accountId: Int,
    val loginUserId: String,
    val pin: String
)
