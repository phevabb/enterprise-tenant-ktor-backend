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


fun Transaction.setTenantSchema(tenantSchema: String) {
    val safeSchema = tenantSchema.replace("\"", "\"\"")
    exec("""SET LOCAL search_path TO "$safeSchema"""")
}




object SubjectRepository {



// ✅ CREATE

    fun assignManyToCategory(subjectIds: List<Int>, categoryId: Int) = transaction {
        SubjectsTable.update({ SubjectsTable.id inList subjectIds }) {
            it[SubjectsTable.category] = org.jetbrains.exposed.dao.id.EntityID(categoryId, CategoriesTable)
        }
    }


    fun create(
        tenantSchema: String,
        req: CreateSubjectRequest
    ): Subject = transaction {

        setTenantSchema(tenantSchema)

        val id = SubjectsTable.insertAndGetId {
            it[name] = req.name.trim()
            it[category] = EntityID(req.categoryId, CategoriesTable)
        }.value

        findById(tenantSchema, id)!!
    }

    // ✅ FIND ALL (FIXED: JOIN added)
    fun findAll(
        tenantSchema: String
    ): List<Subject> = transaction {

        setTenantSchema(tenantSchema)

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
    fun findById(
        tenantSchema: String,
        id: Int
    ): Subject? = transaction {

        setTenantSchema(tenantSchema)

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

    // ✅ DELETE
    fun delete(
        tenantSchema: String,
        id: Int
    ): Boolean = transaction {

        setTenantSchema(tenantSchema)

        SubjectsTable.deleteWhere { SubjectsTable.id eq id } > 0
    }

    // ✅ FIND BY CATEGORY (FIXED)
    fun findByCategory(
        tenantSchema: String,
        categoryId: Int
    ): List<Subject> = transaction {

        setTenantSchema(tenantSchema)

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
    fun update(
        tenantSchema: String,
        id: Int,
        req: CreateSubjectRequest
    ): Subject? = transaction {

        setTenantSchema(tenantSchema)

        val updated = SubjectsTable.update(
            { SubjectsTable.id eq id }
        ) { row ->
            row[name] = req.name.trim()
            row[category] = EntityID(req.categoryId, CategoriesTable)
        }

        if (updated == 0) null else findById(tenantSchema, id)
    }
}