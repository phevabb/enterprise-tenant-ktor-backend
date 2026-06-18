package com.example.staff.dtos.response



import kotlinx.serialization.Serializable

@Serializable
data class CollectionSummaryResponse(
    val academicYear: String,
    val term: String,
    val collectedAmount: Int,
    val pendingAmount: Int
)
