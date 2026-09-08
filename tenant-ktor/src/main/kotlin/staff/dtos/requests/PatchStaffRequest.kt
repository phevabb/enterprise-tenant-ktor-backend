package com.example.staff.dtos.requests

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import others.LocalDateSerializer
import java.time.LocalDate

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
    @Serializable(with = LocalDateSerializer::class)
    val dateOfBirth: LocalDate? = null,
    val nationality: String? = null,
    val role: String? = null,
    val isActive: Boolean? = null,
    val isStaff: Boolean? = null
)
