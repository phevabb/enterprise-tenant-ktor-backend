package com.example.student.dtos.requests
import kotlinx.serialization.Serializable

@Serializable
data class PatchAcademicYearRequest(
    val name: String? = null,
)
