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

    fun create(
        tenantSchema: String,
        req: CreateSubjectCategoryRequest
    ): SubjectCategoryResponse = transaction {

        setTenantSchema(tenantSchema)

        val id = SubjectCategoriesTable.insertAndGetId {
            it[category] = req.categoryId
        }.value

        req.subjectIds.forEach { subjectId ->
            SubjectCategorySubjectsTable.insertIgnore {
                it[subjectCategory] = id
                it[subject] = subjectId
            }
        }

        findById(
            tenantSchema = tenantSchema,
            id = id
        )!!
    }

    fun findById(
        tenantSchema: String,
        id: Int
    ): SubjectCategoryResponse? = transaction {

        setTenantSchema(tenantSchema)

        SubjectCategoriesTable
            .selectAll()
            .where { SubjectCategoriesTable.id eq id }
            .singleOrNull()
            ?.toSubjectCategory()
    }

    fun findAll(
        tenantSchema: String
    ): List<SubjectCategoryResponse> = transaction {

        setTenantSchema(tenantSchema)

        SubjectCategoriesTable
            .selectAll()
            .map { it.toSubjectCategory() }
    }

    fun updateSubjects(
        tenantSchema: String,
        id: Int,
        subjectIds: List<Int>
    ) = transaction {

        setTenantSchema(tenantSchema)

        SubjectCategorySubjectsTable.deleteWhere {
            SubjectCategorySubjectsTable.subjectCategory eq id
        }

        subjectIds.forEach { subjectId ->
            SubjectCategorySubjectsTable.insert {
                it[subjectCategory] = id

                it[subject] =
                    EntityID(subjectId, SubjectsTable)
            }
        }
    }

    fun delete(
        tenantSchema: String,
        id: Int
    ): Boolean = transaction {

        setTenantSchema(tenantSchema)

        SubjectCategorySubjectsTable.deleteWhere {
            SubjectCategorySubjectsTable.subjectCategory eq id
        }

        SubjectCategoriesTable.deleteWhere {
            SubjectCategoriesTable.id eq id
        } > 0
    }
}