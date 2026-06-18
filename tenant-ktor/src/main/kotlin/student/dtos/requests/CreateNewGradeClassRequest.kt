package com.example.student.dtos.requests
import kotlinx.serialization.Serializable

@Serializable
data class CreateNewGradeClassRequest(

    val name: String,
    val categoryId: Int,
    val is_active: Boolean = true

)
