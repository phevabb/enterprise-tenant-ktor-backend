package com.example.academics.repos

import com.example.academics.dtos.requests.CreateSubjectRequest
import com.example.academics.mappers.toSubject
import com.example.academics.models.Subject
import com.example.academics.tables.SubjectsTable
import com.example.academics.tables.CategoriesTable

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.dao.id.EntityID






object SubjectRepository {

    // ✅ CREATE

    fun assignManyToCategory(subjectIds: List<Int>, categoryId: Int) = transaction {
        SubjectsTable.update({ SubjectsTable.id inList subjectIds }) {
            it[SubjectsTable.category] = org.jetbrains.exposed.dao.id.EntityID(categoryId, CategoriesTable)
        }
    }


    fun create(req: CreateSubjectRequest): Subject = transaction {

        val id = SubjectsTable.insertAndGetId {
            it[SubjectsTable.name] = req.name.trim()

            it[SubjectsTable.category] =
                EntityID(req.categoryId, CategoriesTable)
        }.value

        findById(id)!!
    }

    // ✅ FIND ALL (FIXED: JOIN added)
    fun findAll(): List<Subject> = transaction {

        SubjectsTable
            .join(
                CategoriesTable,
                JoinType.LEFT,
                SubjectsTable.category,
                CategoriesTable.id
            )
            .selectAll()
            .orderBy(SubjectsTable.id, SortOrder.DESC)
            .map { it.toSubject() }
    }

    // ✅ FIND BY ID (FIXED)
    fun findById(id: Int): Subject? = transaction {

        SubjectsTable
            .join(
                CategoriesTable,
                JoinType.LEFT,
                SubjectsTable.category,
                CategoriesTable.id
            )
            .selectAll()
            .where { SubjectsTable.id eq id }
            .singleOrNull()
            ?.toSubject()
    }

    // ✅ FIND BY NAME (FIXED)
    fun findByName(name: String): Subject? = transaction {

        SubjectsTable
            .join(
                CategoriesTable,
                JoinType.LEFT,
                SubjectsTable.category,
                CategoriesTable.id
            )
            .selectAll()
            .where { SubjectsTable.name eq name.trim() }
            .singleOrNull()
            ?.toSubject()
    }

    // ✅ EXISTS
    fun existsById(id: Int): Boolean = transaction {
        SubjectsTable
            .selectAll()
            .where { SubjectsTable.id eq id }
            .count() > 0
    }

    // ✅ DELETE
    fun delete(id: Int): Boolean = transaction {
        SubjectsTable.deleteWhere { SubjectsTable.id eq id } > 0
    }

    // ✅ FIND BY CATEGORY (FIXED)
    fun findByCategory(categoryId: Int): List<Subject> = transaction {

        SubjectsTable
            .join(
                CategoriesTable,
                JoinType.LEFT,
                SubjectsTable.category,
                CategoriesTable.id
            )
            .selectAll()
            .where { SubjectsTable.category eq categoryId }
            .map { it.toSubject() }
    }

    // ✅ UPDATE
    fun update(id: Int, req: CreateSubjectRequest): Subject? = transaction {

        val updated = SubjectsTable.update(
            { SubjectsTable.id eq id }
        ) { row ->

            row[SubjectsTable.name] = req.name.trim()
            row[SubjectsTable.category] =
                EntityID(req.categoryId, CategoriesTable)
        }

        if (updated == 0) null else findById(id)
    }
}