package com.example.student.tables

import com.example.academics.tables.CategoriesTable
import org.jetbrains.exposed.dao.id.IntIdTable

object NewGradeClassTable : IntIdTable("new_grade_class") {

    val name = varchar("name", 120)
    val category = reference("category_id", CategoriesTable).nullable()
    val isActive = bool("is_active").default(true)

}