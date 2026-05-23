package com.example.academics.repos



import com.example.academics.dtos.response.SubjectResponse
import com.example.academics.mappers.toSubject
import com.example.academics.mappers.toSubjectCategory
import com.example.academics.models.Subject
import com.example.academics.tables.SubjectsTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object SubjectRepository {

    fun create(name: String): Subject = transaction {
        val id = SubjectsTable.insertAndGetId {
            it[SubjectsTable.name] = name.trim()
        }.value

        findById(id)!!
    }

//    fun findAll(): List<SubjectResponse> = transaction {
//        SubjectsTable
//            .selectAll()
//            .orderBy(SubjectsTable.id, SortOrder.DESC)
//            .map { it.toSubjectCategory() }
//    }

    fun findById(id: Int): Subject? = transaction {
        SubjectsTable
            .selectAll()
            .where { SubjectsTable.id eq id }
            .singleOrNull()
            ?.toSubject()
    }

    fun findByName(name: String): Subject? = transaction {
        SubjectsTable
            .selectAll()
            .where { SubjectsTable.name eq name.trim() }
            .singleOrNull()
            ?.toSubject()
    }

    fun existsById(id: Int): Boolean = transaction {
        SubjectsTable.selectAll().where { SubjectsTable.id eq id }.count() > 0
    }

    fun delete(id: Int): Boolean = transaction {
        SubjectsTable.deleteWhere { SubjectsTable.id eq id } > 0
    }
}