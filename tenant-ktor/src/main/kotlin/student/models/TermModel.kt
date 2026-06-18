package com.example.student.models

import kotlinx.serialization.Serializable

@Serializable
data class TermModel(
    val id: Int,
    val name: String,
    val academic_year: Int,
)
