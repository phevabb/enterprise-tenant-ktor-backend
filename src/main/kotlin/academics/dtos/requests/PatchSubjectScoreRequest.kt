package com.example.academics.dtos.requests



import kotlinx.serialization.Serializable

@Serializable
data class PatchSubjectScoreRequest(
    val classScore: Int? = null,
    val examScore: Int? = null
    // Optional: val position: Int? = null  (only if you want to allow patching ranking)
)
