package com.example.student.dtos.response


import com.example.minimals.FamilyMinimal
import kotlinx.serialization.Serializable

@Serializable
data class StudentProfileResponse(
    val id: Int,
    val user: StudentUserResponse,
    val currentNewGradeClass: GradeClassResponse?,
    val family: FamilyMinimal?,

    val isGraduated: Boolean,
    val lastSchoolAttended: String?,

    val isDiscountedStudent: Boolean,
    val isImmunized: Boolean,
    val allergicFoods: String?,
    val otherRelatedInfo: String?,

    val nameOfFather: String?,
    val nameOfMother: String?,
    val occupationOfFather: String?,

    val occupationOfMother: String?,
    val nationalityOfFather: String?,
    val nationalityOfMother: String?,
    val contactOfFather: String?,
    val contactOfMother: String?,
    val houseNumber: String?,
    val hasAllergies: Boolean
)

@Serializable
data class StudentUserResponse(
    val id: Int,
    val pin: String,
    val userId: String,
    val fullName: String,
    val gender: String?,
    val role: String,
    val isActive: Boolean,
    val dateOfBirth: String?,

    val profilePictureUrl: String? = null,
    val profilePicturePublicId: String? = null

)

@Serializable
data class GradeClassResponse(
    val id: Int,
    val name: String
)





