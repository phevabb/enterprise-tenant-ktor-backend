package com.example.academics.dtos.requests



import kotlinx.serialization.Serializable

@Serializable
data class CreateAcademicRecordRequest(
    val studentId: Int,
    val termId: Int,
    val academicYearId: Int,
    val classLevelId: Int,

    val overallPosition: Int? = null,
    val attendance: String? = null,
    val numberOnRoll: Int = 0
)