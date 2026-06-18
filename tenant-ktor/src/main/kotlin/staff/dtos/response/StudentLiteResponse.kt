package com.example.staff.dtos.response



import kotlinx.serialization.Serializable


@Serializable
data class StudentLiteResponse(
    val id: Int,
    val full_name: String,
    val indexNo: String
)