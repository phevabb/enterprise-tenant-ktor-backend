package com.example.academics.dtos.response

import com.example.academics.models.Subject
import kotlinx.serialization.Serializable

@Serializable
data class SubjectCategoryResponse(
    val id: Int,
    val categoryId: Int,
    val categoryName: String,
    val subjects: List<Subject>   // ✅ full subject objects
)