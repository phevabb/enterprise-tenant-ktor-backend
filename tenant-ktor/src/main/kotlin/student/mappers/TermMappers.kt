package com.example.student.mappers

import com.example.student.models.TermModel
import com.example.student.tables.TermTable
import org.jetbrains.exposed.sql.ResultRow


fun ResultRow.toTermModel() = TermModel(
    id = this[TermTable.id].value,
    name = this[TermTable.name],
    academic_year = this[TermTable.academic_year].value,
)