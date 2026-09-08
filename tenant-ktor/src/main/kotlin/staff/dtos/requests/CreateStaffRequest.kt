package com.example.staff.dtos.requests



import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import others.LocalDateSerializer
import java.time.LocalDate

/**
 * Top‑level request for creating a staff profile.
 * Mirrors Django StaffProfileSerializer logic.
 */
@Serializable
data class CreateStaffRequest(

    val user: CreateUserPart,

    /**
     * Django: assignedClass_id (write-only)
     */
    val assignedClassId: Int? = null,

    val tel: String? = null
)

/**
 * Reuse or duplicate depending on your structure.
 * You can also import the same one from student module if shared.
 */
@Serializable
data class CreateUserPart(
    val fullName: String,

    val gender: String? = null,
    @Serializable(with = LocalDateSerializer::class)
    val dateOfBirth: LocalDate? = null, // ISO‑8601 (YYYY‑MM‑DD)
    val nationality: String? = null,

    val role: String = "staff",
    val isActive: Boolean = true,
    val isStaff: Boolean = true
)
