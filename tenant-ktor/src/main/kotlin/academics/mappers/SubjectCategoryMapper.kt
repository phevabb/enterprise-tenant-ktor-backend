package com.example.academics.mappers






import com.example.academics.dtos.response.SubjectCategoryResponse
import com.example.academics.tables.CategoriesTable
import com.example.academics.tables.SubjectCategorySubjectsTable
import com.example.academics.tables.SubjectsTable
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll
import com.example.academics.tables.SubjectCategoriesTable
fun ResultRow.toSubjectCategory(): SubjectCategoryResponse {

    val id = this[SubjectCategoriesTable.id].value
    val categoryId = this[SubjectCategoriesTable.category].value

    // ✅ Get category name
    val categoryName = CategoriesTable
        .selectAll()
        .where { CategoriesTable.id eq categoryId }
        .single()[CategoriesTable.name]

    // ✅ Get subjects via join
    val subjects = SubjectCategorySubjectsTable
        .join(
            SubjectsTable,
            JoinType.INNER,
            SubjectCategorySubjectsTable.subject,
            SubjectsTable.id
        )
        .selectAll()
        .where { SubjectCategorySubjectsTable.subjectCategory eq id }
        .map { it.toSubject() }

    return SubjectCategoryResponse(
        id = id,
        categoryId = categoryId,
        categoryName = categoryName,
        subjects = subjects
    )
}