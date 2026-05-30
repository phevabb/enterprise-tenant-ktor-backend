package com.example.principal.dtos.responses



import kotlinx.serialization.Serializable

@Serializable
data class ExpectedFeesResponse(
    val className: String,
    val term: String,
    val academicYear: String,
    val expectedAmount: Double,
    val collectedAmount: Double,
    val pendingAmount: Double
)
