package com.example.academics.dtos.response




import com.example.minimals.AcademicYearMinimal
import com.example.minimals.ComplexStudentMinimalDto
import com.example.minimals.GradeClassMinimal

import com.example.minimals.TermMinimal
import com.example.student.models.NewGradeClassModel

import kotlinx.serialization.Serializable

@Serializable
data class AcademicRecordWithScoresResponse(
    val id: Int,
    val student: ComplexStudentMinimalDto,
    val term: TermMinimal,
    val academicYear: AcademicYearMinimal,
    val gradeClass: GradeClassMinimal,

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

    val subjectScores: List<SubjectScoreInlineResponse> = emptyList()
)
