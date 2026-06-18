package com.example.student.mappers

import com.example.student.models.AcademicYearModel
import com.example.student.tables.AcademicYearTable

import org.jetbrains.exposed.sql.ResultRow


fun ResultRow.toAcademicYearModel() = AcademicYearModel(
    id = this[AcademicYearTable.id].value,
    name = this[AcademicYearTable.name],
)