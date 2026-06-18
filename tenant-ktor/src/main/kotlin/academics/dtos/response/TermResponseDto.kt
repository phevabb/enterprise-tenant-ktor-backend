package com.example.academics.dtos.response



import kotlinx.serialization.Serializable

@Serializable
data class TermResponseDto(
    val id: Int,
    val name: String?,
    val academic_year: AcademicYearResponse
)
