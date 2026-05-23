package com.example.academics.mappers


import com.example.academics.tables.SubjectsTable

import com.example.academics.models.Subject
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toSubject() = Subject(
    id = this[SubjectsTable.id].value,
    name = this[SubjectsTable.name]
)