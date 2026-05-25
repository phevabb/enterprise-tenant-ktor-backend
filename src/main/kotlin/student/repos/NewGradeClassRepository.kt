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

    // ✅ CREATE CLASS
    fun create(
        name: String,
        isActive: Boolean,
        categoryId: Int
    ): NewGradeClassModel = transaction {

        val id = NewGradeClassTable.insertAndGetId {
            it[NewGradeClassTable.name] = name.trim()
            it[NewGradeClassTable.isActive] = isActive
            it[NewGradeClassTable.category] = EntityID(categoryId, CategoriesTable)
        }.value

        // ✅ return joined object with categoryName
        findByIdWithCategory(id)!!
    }

    // ✅ FIND BY ID (FIXED: join category)
    fun findById(id: Int): NewGradeClassModel? = transaction {
        findByIdWithCategory(id)
    }

    // ✅ FIND ALL (already correct)
    fun findAll(): List<NewGradeClassModel> = transaction {
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

    // ✅ DELETE
    fun delete(id: Int): Boolean = transaction {
        NewGradeClassTable.deleteWhere { NewGradeClassTable.id eq id } > 0
    }

    // ✅ PATCH / UPDATE (FIXED: return joined fetch)
    fun patch(id: Int, req: PatchNewGradeClassRequest): NewGradeClassModel? = transaction {

        val rowsUpdated = NewGradeClassTable.update({ NewGradeClassTable.id eq id }) { row ->

            req.name?.let { row[NewGradeClassTable.name] = it.trim() }
            req.isActive?.let { row[NewGradeClassTable.isActive] = it }

            req.categoryId?.let {
                row[NewGradeClassTable.category] = EntityID(it, CategoriesTable)
            }
        }

        if (rowsUpdated == 0) null else findByIdWithCategory(id)
    }

    // ✅ ASSIGN SINGLE CLASS → CATEGORY (return updated model if you want)
    fun assignToCategory(classId: Int, categoryId: Int): NewGradeClassModel? = transaction {
        NewGradeClassTable.update({ NewGradeClassTable.id eq classId }) {
            it[NewGradeClassTable.category] = EntityID(categoryId, CategoriesTable)
        }
        findByIdWithCategory(classId)
    }

    // ✅ FIND BY ID WITH CATEGORY (already correct)
    fun findByIdWithCategory(id: Int): NewGradeClassModel? = transaction {
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

    // ✅ ASSIGN MANY CLASSES → CATEGORY
    fun assignManyToCategory(classIds: List<Int>, categoryId: Int): Int = transaction {
        NewGradeClassTable.update({ NewGradeClassTable.id inList classIds }) {
            it[NewGradeClassTable.category] = EntityID(categoryId, CategoriesTable)
        }
    }
}