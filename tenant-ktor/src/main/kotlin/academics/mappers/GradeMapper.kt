package com.example.academics.mappers


import com.example.academics.models.Grade
import com.example.academics.tables.GradesTable

import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toGrade() = Grade(
    id = this[GradesTable.id].value,
    code = this[GradesTable.code],
    label = this[GradesTable.label],
    minScore = this[GradesTable.minScore],
    maxScore = this[GradesTable.maxScore],
    order = this[GradesTable.order]
)