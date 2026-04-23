package com.example.fees.dtos.responses

import com.example.student.dtos.response.AcademicYearResponse
import com.example.student.dtos.response.GradeClassResponse
import com.example.student.dtos.response.TermSimpleResponse
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable


@Serializable
data class FeeStructureResponseDto(
    val id: Int,
    val academic_year: AcademicYearResponse,
    val grade_class: GradeClassResponse,
    val term: TermSimpleResponse,
    @Contextual
    val amount: Int,
    val is_discounted: Boolean
)