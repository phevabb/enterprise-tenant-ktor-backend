package com.example.academics.tables


import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Table

object SubjectCategoriesTable : IntIdTable("subject_categories") {
    val category = reference("category_id", CategoriesTable)
}





object SubjectCategorySubjectsTable : Table("subject_category_subjects") {

    val subjectCategory = reference("subject_category_id", SubjectCategoriesTable)
    val subject = reference("subject_id", SubjectsTable)

    override val primaryKey = PrimaryKey(subjectCategory, subject)
}