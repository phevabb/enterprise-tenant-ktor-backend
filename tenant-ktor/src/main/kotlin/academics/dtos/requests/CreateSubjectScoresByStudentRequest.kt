package com.example.academics.dtos.requests



import kotlinx.serialization.Serializable

@Serializable
data class CreateSubjectScoresByStudentRequest(
    val student: Int,
    val scores: List<CreateSubjectScoreItemRequest>
)

@Serializable
data class CreateSubjectScoreItemRequest(
    val subject: String,
    val classScore: Int? = null,
    val examScore: Int? = null
)