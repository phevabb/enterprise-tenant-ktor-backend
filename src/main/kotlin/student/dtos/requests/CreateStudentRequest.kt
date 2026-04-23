package com.example.student.dtos.requests

import kotlinx.serialization.Serializable

/**
 * Top‑level request for creating a student profile.
 * Matches frontend JSON exactly and is safe for partial / optional fields.
 */
@Serializable
data class CreateStudentRequest(
    val user: CreateUserPart,

    val currentNewGradeClassId: Int? = null,
    val classSeekingAdmissionTo: String? = null,

    val isDiscountedStudent: Boolean = false,
    val isImmunized: Boolean = false,
    val hasAllergies: Boolean = false,

    val allergicFoods: String? = null,
    val lastSchoolAttended: String? = null,
    val otherRelatedInfo: String? = null,

    val nameOfFather: String? = null,
    val occupationOfFather: String? = null,
    val nationalityOfFather: String? = null,
    val contactOfFather: String? = null,

    val nameOfMother: String? = null,
    val occupationOfMother: String? = null,
    val nationalityOfMother: String? = null,
    val contactOfMother: String? = null,

    val houseNumber: String? = null,
    val deactivationReason: String? = null
)

/**
 * Nested user‑creation payload.
 * Kept simple (Strings instead of enums) to avoid serialization friction.
 */
@Serializable
data class CreateUserPart(
    val fullName: String,

    val gender: String? = null,
    val dateOfBirth: String? = null,   // ISO‑8601 string (YYYY‑MM‑DD)
    val nationality: String? = null,

    val role: String = "student",
    val isActive: Boolean = true,
    val isStaff: Boolean = false
)