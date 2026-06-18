package com.example.academics.models


import com.example.student.models.NewGradeClassModel
import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: Int,
    val name: String,
    val classes: List<NewGradeClassModel> = emptyList()
)
