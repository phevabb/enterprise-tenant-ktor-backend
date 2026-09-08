package com.example.student.dtos.requests

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import others.LocalDateSerializer
import java.time.LocalDate

@Serializable
data class PatchStudentRequest(

    val user: PatchUserPart? = null,

    val currentNewGradeClassId: Int? = null,
    val family: Int? = null,
    val classSeekingAdmissionTo: String? = null,

    val isDiscountedStudent: Boolean? = null,
    val isImmunized: Boolean? = null,

    val allergicFoods: String? = null,
    val lastSchoolAttended: String? = null,
    val otherRelatedInfo: String? = null,

    val nameOfFather: String? = null,
    val occupationOfFather: String? = null,
    val nationalityOfFather: String? = null,

    val nameOfMother: String? = null,
    val occupationOfMother: String? = null,
    val nationalityOfMother: String? = null,

    val contactOfFather: String? = null,
    val contactOfMother: String? = null,

    val houseNumber: String? = null,
    val deactivationReason: String? = null
)

@Serializable
data class PatchUserPart(
    val id: Int? = null,
    val fullName: String? = null,
    val gender: String? = null,
    val nationality: String? = null,
    @Serializable(with = LocalDateSerializer::class)
    val dateOfBirth: LocalDate? = null,
    val isActive: Boolean? = null,
    val role: String? = null,
    val isStaff: Boolean? = null
)