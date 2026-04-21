package com.example.student.dtos.requests

import kotlinx.serialization.Serializable

@Serializable
data class CreateStudentRequest(
    val user: CreateUserPart,
    val isDiscountedStudent: Boolean = false,
    val currentNewGradeClassId: Int? = null,
    val lastSchoolAttended: String? = null,
    val isImmunized: Boolean = false,
    val hasAllergies: Boolean = false,
    val allergicFoods: String? = null,
    val hasPeculiarHealthIssues: Boolean = false,
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
data class CreateUserPart(
    val fullName: String,
    val gender: String?,
    val dateOfBirth: String?,
    val nationality: String?,
    val role: String = "student",
    val isActive: Boolean = true,
    val isStaff: Boolean = false
)