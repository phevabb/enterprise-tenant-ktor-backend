package com.example.minimals

import kotlinx.serialization.Serializable

@Serializable
data class FeeStructureMinimal(
    val id: Int,
    val academic_year: AcademicYearMinimal,
    val term: TermMinimal,
    val grade_class: GradeClassMinimal,

)
