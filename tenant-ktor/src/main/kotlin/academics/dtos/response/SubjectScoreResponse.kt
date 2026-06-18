package com.example.academics.dtos.response



import kotlinx.serialization.Serializable

@Serializable
data class SubjectDetailResponse(
    val id: Int,
    val name: String
)

@Serializable
data class SubjectScoreResponse(
    val id: Int,
    val academic_record: Int,
    val subject: Int,
    val subject_detail: SubjectDetailResponse,

    val class_score: Int? = null,
    val exam_score: Int? = null,

    val total_score: Int? = null,
    val grade: Int? = null,
    val interpretation: String? = null,
    val position: Int? = null
)
