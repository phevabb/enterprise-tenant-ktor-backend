package com.example.fees.mappers

import com.example.fees.models.FeeStructureModel
import com.example.fees.tables.FeeStructureTable
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toFeeStructureModel() = FeeStructureModel(
    id = this[FeeStructureTable.id].value,
    academicYearId = this[FeeStructureTable.academic_year].value,
    gradeClassId = this[FeeStructureTable.grade_class].value,
    termId = this[FeeStructureTable.term].value,
    amount = this[FeeStructureTable.amount],
    isDiscounted = this[FeeStructureTable.is_discounted]
)