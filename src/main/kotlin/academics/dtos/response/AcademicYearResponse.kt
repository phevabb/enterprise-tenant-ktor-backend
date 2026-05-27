package com.example.academics.dtos.response



import kotlinx.serialization.Serializable

@Serializable
data class AcademicYearResponse(
    val id: Int,
    val name: String
)