package com.example.student.repos

import com.example.academics.tables.CategoriesTable
import com.example.student.dtos.requests.PatchNewGradeClassRequest
import com.example.student.mappers.toNewGradeClassModel
import com.example.student.models.NewGradeClassModel
import com.example.student.tables.NewGradeClassTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object NewGradeClassRepository {

    fun create(
        tenantSchema: String,
        name: String,
        isActive: Boolean,
        categoryId: Int
    ): NewGradeClassModel = transaction {

        setTenantSchema(tenantSchema)

        val id = NewGradeClassTable.insertAndGetId {
            it[NewGradeClassTable.name] = name.trim()
            it[NewGradeClassTable.isActive] = isActive
            it[NewGradeClassTable.category] =
                EntityID(categoryId, CategoriesTable)
        }.value

        findByIdWithCategory(
            tenantSchema = tenantSchema,
            id = id
        )!!
    }

    fun findById(
        tenantSchema: String,
        id: Int
    ): NewGradeClassModel? = transaction {

        setTenantSchema(tenantSchema)

        findByIdWithCategory(
            tenantSchema = tenantSchema,
            id = id
        )
    }

    fun findAll(
        tenantSchema: String
    ): List<NewGradeClassModel> = transaction {

        setTenantSchema(tenantSchema)


        NewGradeClassTable
            .join(
                CategoriesTable,
                JoinType.LEFT,
                NewGradeClassTable.category,
                CategoriesTable.id
            )
            .selectAll()
            .orderBy(NewGradeClassTable.id, SortOrder.DESC)
            .map { it.toNewGradeClassModel() }
    }

    fun delete(
        tenantSchema: String,
        id: Int
    ): Boolean = transaction {

        setTenantSchema(tenantSchema)

        NewGradeClassTable.deleteWhere {
            NewGradeClassTable.id eq id
        } > 0
    }

    fun patch(
        tenantSchema: String,
        id: Int,
        req: PatchNewGradeClassRequest
    ): NewGradeClassModel? = transaction {

        setTenantSchema(tenantSchema)

        val rowsUpdated = NewGradeClassTable.update({
            NewGradeClassTable.id eq id
        }) { row ->

            req.name?.let {
                row[NewGradeClassTable.name] = it.trim()
            }

            req.isActive?.let {
                row[NewGradeClassTable.isActive] = it
            }

            req.categoryId?.let {
                row[NewGradeClassTable.category] =
                    EntityID(it, CategoriesTable)
            }
        }

        if (rowsUpdated == 0) {
            null
        } else {
            findByIdWithCategory(
                tenantSchema = tenantSchema,
                id = id
            )
        }
    }

    fun findByIdWithCategory(
        tenantSchema: String,
        id: Int
    ): NewGradeClassModel? = transaction {

        setTenantSchema(tenantSchema)

        NewGradeClassTable
            .join(
                CategoriesTable,
                JoinType.LEFT,
                NewGradeClassTable.category,
                CategoriesTable.id
            )
            .selectAll()
            .where { NewGradeClassTable.id eq id }
            .singleOrNull()
            ?.toNewGradeClassModel()
    }
}