package com.example.admin.dtos.response




import kotlinx.serialization.Serializable
import com.example.student.dtos.response.StudentUserResponse
import com.example.student.dtos.response.GradeClassResponse

@Serializable
data class AdminProfileResponse(
    val id: Int,
    val user: StudentUserResponse,

    val tel: String?
)
