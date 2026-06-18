package com.example.student.dtos.requests

import kotlinx.serialization.Serializable

@Serializable
data class CreateClassPromotionRequest(
    val currentStageId: Int,
    val nextStageId: Int?
)