package com.example.student.models
import kotlinx.serialization.Serializable

@Serializable
data class NewGradeClassModel(
    val id: Int,
    val name: String,
    val is_active: Boolean,
    val categoryId: Int?

)


