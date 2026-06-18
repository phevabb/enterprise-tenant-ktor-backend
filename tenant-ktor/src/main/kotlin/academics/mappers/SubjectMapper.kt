package com.example.academics.mappers


import com.example.academics.tables.SubjectsTable

import com.example.academics.models.Subject
import com.example.academics.tables.CategoriesTable
import com.example.student.tables.NewGradeClassTable
import org.jetbrains.exposed.sql.ResultRow



fun ResultRow.toSubject() = Subject(
    id = this[SubjectsTable.id].value,
    name = this[SubjectsTable.name],
    categoryId = this[SubjectsTable.category]?.value,
    categoryName = this.getOrNull(CategoriesTable.name)
)