package com.example.academics.dtos.requests



import kotlinx.serialization.Serializable

@Serializable
data class PatchGradeRequest(
    val code: String? = null,
    val label: String? = null,
    val minScore: Int? = null,
    val maxScore: Int? = null,
    val order: Int? = null
)