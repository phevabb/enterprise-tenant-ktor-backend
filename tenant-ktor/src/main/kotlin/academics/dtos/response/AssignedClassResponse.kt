package com.example.academics.dtos.response

import kotlinx.serialization.Serializable


@Serializable
data class AssignedClassResponse(
    val userId: String,
    val assignedClass: AssignedClassDto? // null if not assigned
)


@Serializable
data class AssignedClassDto(
    val id: Int,
    val name: String,

    )