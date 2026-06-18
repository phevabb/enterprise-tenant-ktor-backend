package com.example.minimals

import kotlinx.serialization.Serializable

@Serializable
data class AcademicRecordMinimal(
    val id: Int,

    val overallPosition: Int?,     // ✅ nullable
    val student: StudentMinimalDto,
    val term: TermMinimal,
    val academicYear: AcademicYearMinimal,
    val classLevel: GradeClassMinimal,

    val attendance: String?,
    val numberOnRoll: Int,

    val conduct: String?,
    val interest: String?,
    val attitude: String?,
    val teacherRemarks: String?,
    val headTeacherRemarks: String?,
    val nextTermBegins: String?,
    val promotedTo: String?,

    val rawScoreTotal: Int?        // ✅ include this
)
