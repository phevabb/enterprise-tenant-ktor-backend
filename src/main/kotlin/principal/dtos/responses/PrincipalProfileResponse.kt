package com.example.principal.dtos.responses



import kotlinx.serialization.Serializable
import com.example.student.dtos.response.StudentUserResponse

@Serializable
data class PrincipalProfileResponse(
    val id: Int,
    val user: StudentUserResponse
)
