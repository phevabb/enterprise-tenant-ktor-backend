package com.example.billing.tables



import com.example.academics.tables.CategoriesTable
import com.example.student.tables.AcademicYearTable
import com.example.student.tables.TermTable
import org.jetbrains.exposed.dao.id.IntIdTable

object BillTemplatesTable : IntIdTable("bill_templates") {

    val name = varchar("name", 150)

    val category = reference("category_id", CategoriesTable)

    val academicYear = reference("academic_year_id", AcademicYearTable)

    val academicTerm = reference("academic_term_id", TermTable)

    val description = text("description").nullable()

    val isActive = bool("is_active").default(true)

    val createdAtEpochMillis = long("created_at_epoch_millis")

    val updatedAtEpochMillis = long("updated_at_epoch_millis").nullable()
}