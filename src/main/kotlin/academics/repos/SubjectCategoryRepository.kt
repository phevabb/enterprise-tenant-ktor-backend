package com.example.academics.repos


import com.example.academics.dtos.requests.CreateSubjectCategoryRequest
import com.example.academics.dtos.response.SubjectCategoryResponse
import com.example.academics.mappers.toSubjectCategory
import com.example.academics.models.SubjectCategory
import com.example.academics.tables.SubjectCategoriesTable
import com.example.academics.tables.SubjectCategorySubjectsTable
import com.example.academics.tables.SubjectCategorySubjectsTable.subjectCategory
import com.example.academics.tables.SubjectsTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object SubjectCategoryRepository {

    fun create(req: CreateSubjectCategoryRequest): SubjectCategoryResponse = transaction {

        // ✅ create subject category
        val id = SubjectCategoriesTable.insertAndGetId {
            it[category] = req.categoryId
        }.value

        // ✅ assign subjects (M2M)
        req.subjectIds.forEach { subjectId ->
            SubjectCategorySubjectsTable.insertIgnore {
                it[subjectCategory] = id
                it[subject] = subjectId
            }
        }

        findById(id)!!
    }

    fun findById(id: Int): SubjectCategoryResponse? = transaction {
        SubjectCategoriesTable
            .selectAll()
            .where { SubjectCategoriesTable.id eq id }
            .singleOrNull()
            ?.toSubjectCategory()
    }


    fun findAll(): List<SubjectCategoryResponse> = transaction {
        SubjectCategoriesTable
            .selectAll()
            .map { it.toSubjectCategory() }   // ✅ now using mapper
    }

    fun updateSubjects(id: Int, subjectIds: List<Int>) = transaction {

        // ✅ remove old
        SubjectCategorySubjectsTable.deleteWhere {
            SubjectCategorySubjectsTable.subjectCategory eq id
        }

        // ✅ insert new


                subjectIds.forEach { subjectId ->
                    SubjectCategorySubjectsTable.insert {
                        it[SubjectCategorySubjectsTable.subjectCategory] = id

                        it[SubjectCategorySubjectsTable.subject] =
                            EntityID(subjectId, SubjectsTable)   // ✅ FIXED
                    }
                }


    }

    fun delete(id: Int): Boolean = transaction {

        SubjectCategorySubjectsTable.deleteWhere {
            SubjectCategorySubjectsTable.subjectCategory eq id
        }

        SubjectCategoriesTable.deleteWhere {
            SubjectCategoriesTable.id eq id
        } > 0
    }
}