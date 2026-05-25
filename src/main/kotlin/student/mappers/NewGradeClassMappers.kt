package com.example.student.mappers

import com.example.student.models.NewGradeClassModel
import com.example.student.tables.NewGradeClassTable
import org.jetbrains.exposed.sql.ResultRow

import com.example.academics.tables.CategoriesTable

fun ResultRow.toNewGradeClassModel() = NewGradeClassModel(
    id = this[NewGradeClassTable.id].value,
    name = this[NewGradeClassTable.name],
    is_active = this[NewGradeClassTable.isActive],

    categoryId = this[NewGradeClassTable.category]?.value,

    categoryName = this.getOrNull(CategoriesTable.name))