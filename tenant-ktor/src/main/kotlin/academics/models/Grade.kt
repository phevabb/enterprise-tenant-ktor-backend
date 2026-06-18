package com.example.academics.models



import kotlinx.serialization.Serializable

@Serializable
data class Grade(
    val id: Int,
    val code: String,
    val label: String,
    val minScore: Int,
    val maxScore: Int,
    val order: Int
)