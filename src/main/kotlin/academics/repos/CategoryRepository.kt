package com.example.academics.repos

import com.example.academics.dtos.response.CategoryResponse
import com.example.academics.dtos.response.GradeClassResponse
import com.example.academics.dtos.response.SubjectCategoryResponse
import com.example.academics.dtos.response.SubjectResponse
import com.example.academics.models.Category
import com.example.academics.tables.CategoriesTable
import com.example.academics.tables.SubjectCategoriesTable
import com.example.academics.tables.SubjectCategorySubjectsTable
import com.example.academics.tables.SubjectsTable
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

import com.example.student.tables.NewGradeClassTable
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.update

object CategoryRepository {

    fun findAll(): List<CategoryResponse> = transaction {

        CategoriesTable.selectAll().map { categoryRow ->

            val categoryId = categoryRow[CategoriesTable.id].value
            val categoryName = categoryRow[CategoriesTable.name]

            // ✅ 1. specific_classes (1 → many)
            val classes = NewGradeClassTable
                .selectAll()
                .where { NewGradeClassTable.category eq categoryId }
                .map {
                    GradeClassResponse(
                        id = it[NewGradeClassTable.id].value,
                        name = it[NewGradeClassTable.name]
                    )
                }



            CategoryResponse(
                id = categoryId,
                name = categoryName,
                specific_classes = classes,

            )
        }
    }

    fun create(name: String): Category = transaction {
        val id = CategoriesTable.insertAndGetId {
            it[CategoriesTable.name] = name.trim()
        }.value

        Category(id, name)
    }

    fun delete(id: Int): Boolean = transaction {

        // ✅ Remove category from all classes
        NewGradeClassTable.update(
            { NewGradeClassTable.category eq id }
        ) {
            it[NewGradeClassTable.category] = null
        }

        CategoriesTable.deleteWhere {
            CategoriesTable.id eq id
        } > 0
    }

}

