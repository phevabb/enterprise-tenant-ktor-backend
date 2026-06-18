package com.example.academics.dtos.response



import kotlinx.serialization.Serializable

@Serializable
data class SubjectScoreContextResponse(
    val id: Int,
    val academicRecordId: Int,
    val studentId: Int,
    val subjectId: Int,
    val subjectName: String,
    val classScore: Int? = null,
    val examScore: Int? = null,
    val totalScore: Int? = null,
    val gradeCode: String? = null,
    val position: Int? = null
)
