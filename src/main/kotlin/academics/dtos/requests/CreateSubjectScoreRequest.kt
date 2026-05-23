package com.example.academics.dtos.requests


import kotlinx.serialization.Serializable

@Serializable
data class CreateSubjectScoreRequest(
    val academicRecordId: Int,
    val subjectId: Int,
    val classScore: Int? = null,
    val examScore: Int? = null
)