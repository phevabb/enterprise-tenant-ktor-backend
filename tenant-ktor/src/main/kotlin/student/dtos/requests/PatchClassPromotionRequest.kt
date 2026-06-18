package com.example.student.dtos.requests



import kotlinx.serialization.Serializable

@Serializable
data class PatchClassPromotionRequest(
    val currentStageId: Int? = null,

    /**
     * If setNextStage=true:
     * - nextStageId = some Int => update
     * - nextStageId = null => clear (Graduated)
     */
    val setNextStage: Boolean = false,
    val nextStageId: Int? = null
)