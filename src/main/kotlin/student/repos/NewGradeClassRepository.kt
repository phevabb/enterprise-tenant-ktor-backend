package com.example.student.repos

import com.example.student.dtos.requests.PatchNewGradeClassRequest
import com.example.student.mappers.toNewGradeClassModel
import com.example.student.models.NewGradeClassModel
import com.example.student.tables.NewGradeClassTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.update

object NewGradeClassRepository {
    // create class

    fun create(name: String, isActive: Boolean): NewGradeClassModel = transaction {
        val id = NewGradeClassTable.insertAndGetId {
            it[NewGradeClassTable.name] = name
            it[NewGradeClassTable.isActive] = isActive
        }.value

        NewGradeClassModel(
            id = id,
            name = name,
            is_active = isActive
        )
    }

    fun findById(id: Int): NewGradeClassModel? = transaction {
        NewGradeClassTable
            .selectAll()
            .where { NewGradeClassTable.id eq id }
            .singleOrNull()
        ?.toNewGradeClassModel()
    }

    fun findAll(): List<NewGradeClassModel> = transaction {
        NewGradeClassTable
            .selectAll()
            .orderBy(NewGradeClassTable.id, SortOrder.DESC)
            .map { it.toNewGradeClassModel() }
    }

    fun delete(id: Int) = transaction {
        NewGradeClassTable.deleteWhere {
            NewGradeClassTable.id eq id
        } > 0
    }

    fun patch(id: Int, req: PatchNewGradeClassRequest): NewGradeClassModel? = transaction {

        val rowsUpdated = NewGradeClassTable.update(
            where = { NewGradeClassTable.id eq id }
        ) { row ->
            req.name?.let { row[NewGradeClassTable.name] = it }
            req.isActive?.let { row[NewGradeClassTable.isActive] = it }
        }

        if (rowsUpdated == 0) {
            // ❌ nothing updated → record not found
            null
        } else {
            // ✅ fetch and return the updated object
            findById(id)!!
        }
    }




}


