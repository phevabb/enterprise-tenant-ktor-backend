package com.example.academics.dtos.requests



import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateSubjectScoreByStudentRequest(
    val student: Int,

    /**
     * Accepts:
     * - "1" (id as string)
     * - 1 (if frontend sends number -> it will still serialize as number in JSON; Kotlin expects String? if you want both)
     * - "Mathematics" (name)
     */
    val subject: String,

    @SerialName("class_score")
    val classScore: Int? = null,

    @SerialName("exam_score")
    val examScore: Int? = null
)
