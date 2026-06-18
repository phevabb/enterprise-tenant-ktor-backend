package com.example.fees.dtos.responses

import com.example.student.dtos.response.AcademicYearResponse
import com.example.student.dtos.response.GradeClassResponse


import com.example.fees.tables.FeeStructureTable
import com.example.student.dtos.response.TermSimpleResponse
import com.example.student.tables.*
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toFeeStructureResponseDto() = FeeStructureResponseDto(
    id = this[FeeStructureTable.id].value,

    academic_year = AcademicYearResponse(
        id = this[AcademicYearTable.id].value,
        name = this[AcademicYearTable.name]
    ),

    grade_class = GradeClassResponse(
        id = this[NewGradeClassTable.id].value,
        name = this[NewGradeClassTable.name]
    ),

    term = TermSimpleResponse(
        id = this[TermTable.id].value,
        name = this[TermTable.name]
    ),


    amount = this[FeeStructureTable.amount],
    is_discounted = this[FeeStructureTable.is_discounted]



)