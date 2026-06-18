package com.example.academics.dtos.response

import com.example.minimals.AcademicRecordMinimal
import kotlinx.serialization.Serializable

@Serializable
data class SubjectScoreExpandedResponse(
    val id: Int,
    val academicRecord: AcademicRecordMinimal,  // ✅ object => name should NOT end with Id
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