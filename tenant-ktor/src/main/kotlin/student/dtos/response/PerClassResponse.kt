package com.example.student.dtos.response

import kotlinx.serialization.Serializable

@Serializable
data class PerClassResponse(
    val `class`: String,
    val count: Long
)
