package com.example.academics.dtos.requests



import kotlinx.serialization.Serializable

@Serializable
data class NewCreateAcademicRecordRequest(
    val studentId: Int,
    val termId: Int,
    val academicYearId: Int,
    val classLevelId: Int
)

