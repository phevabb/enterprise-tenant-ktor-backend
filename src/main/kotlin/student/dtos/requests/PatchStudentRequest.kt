package com.example.student.dtos.requests


import kotlinx.serialization.Serializable

@Serializable
data class PatchStudentRequest(
    // Nested user/account update (optional)
    val user: PatchUserPart? = null,

    // Profile fields (all optional for PATCH)
    val currentNewGradeClassId: Int? = null,
    val isGraduated: Boolean? = null,
    val lastSchoolAttended: String? = null,

    val isDiscountedStudent: Boolean? = null,
    val isImmunized: Boolean? = null,
    val hasAllergies: Boolean? = null,
    val hasPeculiarHealthIssues: Boolean? = null,

    val allergicFoods: String? = null,
    val healthIssues: String? = null,
    val otherRelatedInfo: String? = null,

    val nameOfFather: String? = null,
    val nameOfMother: String? = null,
    val occupationOfFather: String? = null,
    val occupationOfMother: String? = null,

    val nationalityOfFather: String? = null,
    val nationalityOfMother: String? = null,
    val contactOfFather: String? = null,
    val contactOfMother: String? = null,

    val houseNumber: String? = null
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