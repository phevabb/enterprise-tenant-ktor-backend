package com.example.student.models

import kotlinx.serialization.Serializable

@Serializable
data class StudentProfile(
    val id: Int,
    val user: Int,
    val currentNewGradeClassId: Int?,

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