package com.example.academics.dtos.response

// trying to combine two tables for one person
import kotlinx.serialization.Serializable

@Serializable
data class AcademicRecordDetailResponse(
    val id: Int,
    val studentId: Int,
    val termId: Int,
    val academicYearId: Int,
    val classLevelId: Int,

    val overallPosition: Int? = null,
    val attendance: String? = null,
    val numberOnRoll: Int = 0,

    val conduct: String? = null,
    val interest: String? = null,
    val attitude: String? = null,
    val teacherRemarks: String? = null,
    val headTeacherRemarks: String? = null,
    val nextTermBegins: String? = null,
    val promotedTo: String? = null,

    val rawScoreTotal: Int? = null,

    // ✅ "inline" part
    val subjectScores: List<SubjectScoreInlineResponse> = emptyList()
)

@Serializable
data class SubjectScoreInlineResponse(
    val id: Int,
    val subjectId: Int,
    val subjectName: String,

    val classScore: Int? = null,
    val examScore: Int? = null,
    val totalScore: Int? = null,

    val gradeId: Int? = null,
    val gradeCode: String? = null,
    val gradeLabel: String? = null,

    val interpretation: String? = null,
    val position: Int? = null
)
