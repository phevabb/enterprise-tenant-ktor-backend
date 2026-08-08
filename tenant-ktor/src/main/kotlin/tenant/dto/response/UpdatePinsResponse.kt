package com.example.tenant.dto.response


import kotlinx.serialization.Serializable

@Serializable
data class UpdatePinsResponse(
    val success: Boolean,
    val message: String
)