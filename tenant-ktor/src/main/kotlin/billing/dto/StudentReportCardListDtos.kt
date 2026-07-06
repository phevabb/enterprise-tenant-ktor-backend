package com.example.billing.dto



import kotlinx.serialization.Serializable

@Serializable
data class StudentReportCardListItemResponse(
    val id: Int,
    val studentName: String,
    val studentUserId: String,
    val profilePictureUrl: String?,
    val academicYearName: String,
    val termName: String,
    val className: String,
    val rawScoreTotal: Int?,
    val averageScore: Double,
    val overallPosition: Int?,
    val numberOnRoll: Int?,
    val subjectCount: Int
)