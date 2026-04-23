package com.example.student.dtos.requests

import kotlinx.serialization.Serializable

@Serializable
data class PatchTermRequest(
    val name: String? = null,
    val academic_year_id: Int? = null,

)
