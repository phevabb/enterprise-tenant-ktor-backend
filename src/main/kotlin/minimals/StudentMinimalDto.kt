package com.example.minimals

import kotlinx.serialization.Serializable

@Serializable
data class StudentMinimalDto(
    val id: Int,
    val name: String
)