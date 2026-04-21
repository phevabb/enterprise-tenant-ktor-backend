package com.example.student.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object NewGradeClassTable : IntIdTable("new_grade_class") {

    val name = varchar("name", 120)
    val isActive = bool("is_active").default(true)

}