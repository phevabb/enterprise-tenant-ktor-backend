package com.example.student.dtos.response

import com.example.minimals.GradeClassMinimal
import kotlinx.serialization.Serializable

@Serializable
data class ClassPromotionResponseDto(
    val id: Int,
    val currentStage: GradeClassResponse,
    val nextStage: GradeClassResponse?,
    val promotionPath: String
)