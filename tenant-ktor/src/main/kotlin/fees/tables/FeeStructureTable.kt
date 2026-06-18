package com.example.fees.tables

import com.example.student.tables.AcademicYearTable
import com.example.student.tables.NewGradeClassTable
import com.example.student.tables.TermTable
import org.jetbrains.exposed.dao.id.IntIdTable

object FeeStructureTable : IntIdTable("fee_structure") {
    val academic_year = reference("academic_year", AcademicYearTable)
    val grade_class = reference("grade_class", NewGradeClassTable)
    val term = reference("term", TermTable)
    val amount = integer("amount")          // ✅ whole cedis
    val is_discounted = bool("is_discounted").default(false)
}






