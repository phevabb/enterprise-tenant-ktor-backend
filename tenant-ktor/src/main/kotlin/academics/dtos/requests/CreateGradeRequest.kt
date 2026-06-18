package com.example.academics.dtos.requests



import kotlinx.serialization.Serializable

@Serializable
data class CreateGradeRequest(
    val code: String,
    val label: String,
    val minScore: Int,
    val maxScore: Int,
    val order: Int
)