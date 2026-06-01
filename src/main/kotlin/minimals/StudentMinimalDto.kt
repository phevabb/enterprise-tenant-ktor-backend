package com.example.minimals

import kotlinx.serialization.Serializable

@Serializable
data class StudentMinimalDto(
    val id: Int,
    val name: String
)





@Serializable
data class ComplexStudentMinimalDto(
    val id: Int,
    val name: String,
    val userId: String,
    val profilePictureUrl: String? = null,
    val profilePicturePublicId: String? = null



)