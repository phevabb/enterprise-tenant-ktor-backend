package com.example.fees.models
import kotlinx.serialization.Serializable


@Serializable
data class FeeStructureModel(
    val id: Int,
    val academicYearId: Int,
    val gradeClassId: Int,
    val termId: Int,
    val amount: Int,      // ✅ whole cedis
    val isDiscounted: Boolean
)