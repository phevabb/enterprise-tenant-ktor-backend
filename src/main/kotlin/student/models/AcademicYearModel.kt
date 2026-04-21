package com.example.student.models
import kotlinx.serialization.Serializable


@Serializable
data class AcademicYearModel(
    val id: Int,
    val name: String,
)
