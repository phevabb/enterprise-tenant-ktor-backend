package com.example.academics.dtos.response


import com.example.minimals.AcademicYearMinimal
import com.example.minimals.ComplexStudentMinimalDto
import com.example.minimals.GradeClassMinimal
import com.example.minimals.TermMinimal
import kotlinx.serialization.Serializable

@Serializable
data class StudentReportCardResponse(
    val id: Int,

    val student: ComplexStudentMinimalDto,
    val term: TermMinimal,
    val academicYear: AcademicYearMinimal,
    val classLevel: GradeClassMinimal,

    val overallPosition: Int? = null,
    val rawScoreTotal: Int? = null,
    val promotedTo: String? = null,
    val numberOnRoll: Int = 0,
    val attendance: String? = null,

    // remarks fields
    val conduct: String? = null,
    val interest: String? = null,
    val attitude: String? = null,
    val teacherRemarks: String? = null,
    val headTeacherRemarks: String? = null,
    val nextTermBegins: String? = null,

    // nested subjects
    val subjects: List<SubjectScoreInlineResponse> = emptyList()
)

