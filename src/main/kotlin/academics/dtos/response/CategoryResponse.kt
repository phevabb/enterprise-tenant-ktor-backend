package com.example.academics.dtos.response

import kotlinx.serialization.Serializable

@Serializable
data class CategoryResponse(
    val id: Int,
    val name: String,
    val specific_classes: List<GradeClassResponse>,
    val subject_groups: List<SubjectCategoryResponse>
)
