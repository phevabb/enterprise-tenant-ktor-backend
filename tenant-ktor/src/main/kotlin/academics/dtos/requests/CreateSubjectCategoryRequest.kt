package com.example.academics.dtos.requests



import kotlinx.serialization.Serializable

@Serializable
data class CreateSubjectCategoryRequest(
    val categoryId: Int,
    val subjectIds: List<Int>
)