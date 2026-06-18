package com.example.staff.dtos.requests

import kotlinx.serialization.Serializable

@Serializable
data class PatchStaffRequest(

    val assignedClassId: Int? = null,
    val tel: String? = null,

    val user: PatchUserPart? = null
)

@Serializable
data class PatchUserPart(
    val fullName: String? = null,
    val gender: String? = null,
    val dateOfBirth: String? = null,
    val nationality: String? = null,
    val role: String? = null,
    val isActive: Boolean? = null,
    val isStaff: Boolean? = null
)
