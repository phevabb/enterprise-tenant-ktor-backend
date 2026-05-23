package com.example.academics.models


import kotlinx.serialization.Serializable

@Serializable
data class SubjectCategory(
    val id: Int,
    val categoryId: Int,
    val subjectIds: List<Int> = emptyList()
)