package com.example.academics.dtos.response



import kotlinx.serialization.Serializable

@Serializable
data class AcademicRecordResponse(
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

    val rawScoreTotal: Int? = null
)
