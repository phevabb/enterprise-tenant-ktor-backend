package com.example.academics.models


import kotlinx.serialization.Serializable

@Serializable
data class SubjectScore(
    val id: Int,

    val academicRecordId: Int,
    val subjectId: Int,

    val position: Int? = null,

    val classScore: Int? = null,
    val examScore: Int? = null,
    val totalScore: Int? = null,

    val gradeId: Int? = null,
    val interpretation: String? = null
)