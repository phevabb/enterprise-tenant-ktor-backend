package com.example.familyfees.dtos.responses

import com.example.minimals.StudentMinimalDto
import kotlinx.serialization.Serializable

@Serializable
data class FamilyResponseDto(
    val id: Int,
    val name: String,
    val is_active: Boolean,
    val members: List<StudentMinimalDto>
)
