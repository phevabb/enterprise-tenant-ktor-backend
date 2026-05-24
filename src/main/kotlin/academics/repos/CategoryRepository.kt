package com.example.academics.repos


import com.example.academics.dtos.response.CategoryResponse
import com.example.academics.dtos.response.GradeClassResponse
import com.example.academics.dtos.response.SubjectResponse
import com.example.academics.models.Category
import com.example.academics.tables.CategoriesTable
import com.example.academics.tables.SubjectsTable
import com.example.student.tables.NewGradeClassTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object CategoryRepository {

    fun findAll(): List<CategoryResponse> = transaction {
        CategoriesTable.selectAll().map { catRow ->
            val categoryId = catRow[CategoriesTable.id].value
            val categoryName = catRow[CategoriesTable.name]

            val classes = NewGradeClassTable
                .selectAll()
                .where { NewGradeClassTable.category eq categoryId }
                .orderBy(NewGradeClassTable.name to SortOrder.ASC)
                .map {
                    GradeClassResponse(
                        id = it[NewGradeClassTable.id].value,
                        name = it[NewGradeClassTable.name]
                    )
                }

            val subjects = SubjectsTable
                .selectAll()
                .where { SubjectsTable.category eq categoryId }
                .orderBy(SubjectsTable.name to SortOrder.ASC)
                .map {
                    SubjectResponse(
                        id = it[SubjectsTable.id].value,
                        name = it[SubjectsTable.name]
                    )
                }

            CategoryResponse(
                id = categoryId,
                name = categoryName,
                specific_classes = classes,
                subjects = subjects
            )
        }
    }

    fun create(name: String): Category = transaction {
        val id = CategoriesTable.insertAndGetId {
            it[CategoriesTable.name] = name.trim()
        }.value
        Category(id, name.trim())
    }

    fun updateName(id: Int, name: String): Category? = transaction {
        val updated = CategoriesTable.update({ CategoriesTable.id eq id }) {
            it[CategoriesTable.name] = name.trim()
        }
        if (updated == 0) null else Category(id, name.trim())
    }

    /**
     * PROTECT delete: only delete if no classes and no subjects use this category.
     */
    fun deleteIfUnused(id: Int): Boolean = transaction {
        val usedByClasses = NewGradeClassTable.selectAll()
            .where { NewGradeClassTable.category eq id }
            .count() > 0

        val usedBySubjects = SubjectsTable.selectAll()
            .where { SubjectsTable.category eq id }
            .count() > 0

        if (usedByClasses || usedBySubjects) return@transaction false

        CategoriesTable.deleteWhere { CategoriesTable.id eq id } > 0
    }
}

