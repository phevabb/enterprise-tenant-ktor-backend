package com.example.student.repos

import com.example.academics.tables.CategoriesTable
import com.example.student.dtos.requests.PatchNewGradeClassRequest
import com.example.student.mappers.toNewGradeClassModel
import com.example.student.models.NewGradeClassModel
import com.example.student.tables.NewGradeClassTable



import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

object NewGradeClassRepository {

    // ✅ CREATE CLASS
    fun create(
        name: String,
        isActive: Boolean,
        categoryId: Int
    ): NewGradeClassModel = transaction {

        val id = NewGradeClassTable.insertAndGetId {
            it[NewGradeClassTable.name] = name.trim()
            it[NewGradeClassTable.isActive] = isActive

            // ✅ FIX FK
            it[NewGradeClassTable.category] =
                EntityID(categoryId, CategoriesTable)
        }.value

        NewGradeClassModel(
            id = id,
            name = name,
            is_active = isActive,
            categoryId = categoryId
        )
    }

    // ✅ FIND BY ID
    fun findById(id: Int): NewGradeClassModel? = transaction {
        NewGradeClassTable
            .selectAll()
            .where { NewGradeClassTable.id eq id }
            .singleOrNull()
            ?.toNewGradeClassModel()
    }

    // ✅ FIND ALL
    fun findAll(): List<NewGradeClassModel> = transaction {
        NewGradeClassTable
            .selectAll()
            .orderBy(NewGradeClassTable.id, SortOrder.DESC)
            .map { it.toNewGradeClassModel() }
    }

    // ✅ DELETE
    fun delete(id: Int): Boolean = transaction {
        NewGradeClassTable.deleteWhere {
            NewGradeClassTable.id eq id
        } > 0
    }

    // ✅ PATCH / UPDATE
    fun patch(id: Int, req: PatchNewGradeClassRequest): NewGradeClassModel? = transaction {

        val rowsUpdated = NewGradeClassTable.update(
            where = { NewGradeClassTable.id eq id }
        ) { row ->

            req.name?.let {
                row[NewGradeClassTable.name] = it.trim()
            }

            req.isActive?.let {
                row[NewGradeClassTable.isActive] = it
            }

            // ✅ OPTIONAL: update category
            req.categoryId?.let {
                row[NewGradeClassTable.category] =
                    EntityID(it, CategoriesTable)
            }
        }

        if (rowsUpdated == 0) {
            null
        } else {
            findById(id)
        }
    }

    // ✅ ASSIGN SINGLE CLASS → CATEGORY
    fun assignToCategory(classId: Int, categoryId: Int) = transaction {
        NewGradeClassTable.update(
            { NewGradeClassTable.id eq classId }
        ) {
            it[NewGradeClassTable.category] =
                EntityID(categoryId, CategoriesTable)
        }
    }

    // ✅ ASSIGN MANY CLASSES → CATEGORY (VERY USEFUL)
    fun assignManyToCategory(classIds: List<Int>, categoryId: Int) = transaction {
        NewGradeClassTable.update(
            { NewGradeClassTable.id inList classIds }
        ) {
            it[NewGradeClassTable.category] =
                EntityID(categoryId, CategoriesTable)
        }
    }
}
