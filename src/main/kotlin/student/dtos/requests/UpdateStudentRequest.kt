package com.example.student.dtos.requests

// put
import kotlinx.serialization.Serializable

@Serializable
data class UpdateStudentRequest(
    val currentNewGradeClassId: Int? = null,
    val isGraduated: Boolean = false,
    val lastSchoolAttended: String? = null,

    val isDiscountedStudent: Boolean = false,
    val isImmunized: Boolean = false,
    val hasAllergies: Boolean = false,
    val hasPeculiarHealthIssues: Boolean = false,

    val allergicFoods: String? = null,
    val healthIssues: String? = null,
    val otherRelatedInfo: String? = null,

    val nameOfFather: String? = null,
    val nameOfMother: String? = null,
    val occupationOfFather: String? = null,
    val occupationOfMother: String? = null,
    val nationalityOfFather: String? = null,
    val nationalityOfMother: String? = null,
    val contactOfFather: String,
    val contactOfMother: String? = null,

    val houseNumber: String? = null
)