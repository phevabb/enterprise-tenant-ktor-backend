package com.example.student.dtos.requests

import kotlinx.serialization.Serializable


@Serializable
data class PatchNewGradeClassRequest(
    val name: String? = null,
    val categoryId: Int? = null,
    val isActive: Boolean? = null
)
