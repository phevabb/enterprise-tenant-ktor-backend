package com.example.fees.dtos.responses

import kotlinx.serialization.Serializable

@Serializable
data class ArrearsResponse(
    val studentId: Int,
    val academicYearId: Int,
    val totalArrears: Int
)