package com.example.familyfees.dtos.responses

import kotlinx.serialization.Serializable

@Serializable
data class FamilyResponseDto(
    val id: Int,
    val name: String,
)
