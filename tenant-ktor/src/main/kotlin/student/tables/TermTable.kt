package com.example.student.tables
import org.jetbrains.exposed.dao.id.IntIdTable

object TermTable: IntIdTable ("term") {

    val name = varchar("name", 50)
    val academic_year = reference("academic_year", AcademicYearTable)
    val isCurrent = bool("is_current").default(false)


}