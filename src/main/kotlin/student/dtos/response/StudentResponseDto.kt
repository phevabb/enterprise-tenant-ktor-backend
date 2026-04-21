package com.example.student.dtos.response


import kotlinx.serialization.Serializable

@Serializable
data class StudentProfileResponse(
    val id: Int,
    val user: StudentUserResponse,
    val currentNewGradeClass: GradeClassResponse?,

    val isGraduated: Boolean,
    val lastSchoolAttended: String?,

    val isDiscountedStudent: Boolean,
    val isImmunized: Boolean,
    val hasAllergies: Boolean,
    val hasPeculiarHealthIssues: Boolean,

    val allergicFoods: String?,
    val healthIssues: String?,
    val otherRelatedInfo: String?,

    val nameOfFather: String?,
    val nameOfMother: String?,
    val occupationOfFather: String?,
    val occupationOfMother: String?,
    val nationalityOfFather: String?,
    val nationalityOfMother: String?,
    val contactOfFather: String?,
    val contactOfMother: String?,

    val houseNumber: String?
)

@Serializable
data class StudentUserResponse(
    val id: Int,
    val userId: String,
    val fullName: String,
    val gender: String?,
    val role: String,
    val isActive: Boolean
)

@Serializable
data class GradeClassResponse(
    val id: Int,
    val name: String
)





