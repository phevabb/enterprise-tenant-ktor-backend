package com.example.student.tables
import org.jetbrains.exposed.dao.id.IntIdTable


object AcademicYearTable: IntIdTable("academic_year") {
    val name = varchar("name", 20).uniqueIndex()
    val isCurrent = bool("is_current").default(false)


}