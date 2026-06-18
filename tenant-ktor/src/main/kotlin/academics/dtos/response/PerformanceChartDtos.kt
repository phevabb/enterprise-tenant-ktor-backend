package com.example.academics.dtos.response

import kotlinx.serialization.Serializable

@Serializable
data class ChartSubjectStat(
    val name: String,
    val score: Int,
    val best: Int,
    val average: Double,
    val worst: Int
)

@Serializable
data class ChartRecordResponse(
    val student: String,
    val `class`: String,
    val position: Int?,
    val classAvg: Int,
    val subjects: List<ChartSubjectStat>
)
