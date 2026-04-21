package com.example.student.dtos.requests
import kotlinx.serialization.Serializable


@Serializable
data class CreateAcademicYearRequest(
    val name: String,
)
