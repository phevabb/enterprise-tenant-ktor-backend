package com.example.student.dtos.response

import kotlinx.serialization.Serializable

@Serializable
data class TermSimpleResponse(
    val id: Int,
    val name: String?
)