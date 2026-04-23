package com.example.student.dtos.requests
import kotlinx.serialization.Serializable


@Serializable
data class CreateTermRequest(
    val name: String,
    val academic_year: Int? = null,
)



@Serializable
data class CreateTermIncoming(
    val name: String,
    val academic_year_id: String
)