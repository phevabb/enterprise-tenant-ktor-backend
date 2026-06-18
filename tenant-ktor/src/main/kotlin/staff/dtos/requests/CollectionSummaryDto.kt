package com.example.staff.dtos.requests



import kotlinx.serialization.Serializable


@Serializable
data class CollectionSummaryDto(
    val academicYear: String,
    val term: String,
    val collectedAmount: Int,
    val pendingAmount: Int
)
