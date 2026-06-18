package com.example.academics.dtos.requests


import kotlinx.serialization.Serializable

@Serializable
data class CreateOrUpdateSubjectScoreRequest(
    val academicRecordId: Int,
    val subjectId: Int,
    val classScore: Int? = null,
    val examScore: Int? = null
)