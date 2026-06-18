package com.example.academics.dtos.requests


import kotlinx.serialization.Serializable

@Serializable
data class CreateSubjectRequest(
    val name: String,
    val categoryId: Int,
)