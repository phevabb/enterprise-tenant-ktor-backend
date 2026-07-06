package com.example.academics.dtos.requests



import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateBulkSubjectScoreBySubjectRequest(
    val subject: String,
    val scores: List<BulkStudentSubjectScoreItemRequest>
)

@Serializable
data class BulkStudentSubjectScoreItemRequest(
    val student: Int,

    @SerialName("class_score")
    val classScore: Int? = null,

    @SerialName("exam_score")
    val examScore: Int? = null
)
