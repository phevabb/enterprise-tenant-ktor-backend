package com.example.staff.dtos.response

import kotlinx.serialization.Serializable
import com.example.student.dtos.response.StudentUserResponse
import com.example.student.dtos.response.GradeClassResponse

@Serializable
data class StaffProfileResponse(
    val id: Int,
    val user: StudentUserResponse,
    val assignedClass: GradeClassResponse?,
    val tel: String?
)
