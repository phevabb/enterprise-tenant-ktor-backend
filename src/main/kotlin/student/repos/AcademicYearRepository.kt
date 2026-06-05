package com.example.student.repos

import com.example.account.AccountTable
import com.example.student.StudentsTable
import com.example.student.dtos.requests.PatchAcademicYearRequest
import com.example.student.mappers.toAcademicYearModel
import com.example.student.models.AcademicYearModel
import com.example.student.tables.AcademicYearTable
import com.example.student.tables.NewClassPromotionTable
import com.example.student.tables.NewGradeClassTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

object AcademicYearRepository {

    fun create(
        tenantSchema: String,
        name: String
    ): AcademicYearModel = transaction {

        setTenantSchema(tenantSchema)

        val id = AcademicYearTable.insertAndGetId {
            it[AcademicYearTable.name] = name
        }.value

        StudentPromotionRepository.promoteAllStudentsForNewAcademicYear()

        AcademicYearModel(
            id = id,
            name = name
        )
    }

    fun findAll(
        tenantSchema: String
    ): List<AcademicYearModel> = transaction {

        setTenantSchema(tenantSchema)


        AcademicYearTable
            .selectAll()
            .orderBy(AcademicYearTable.id, SortOrder.DESC)
            .map { it.toAcademicYearModel() }
    }

    fun getCurrentId(
        tenantSchema: String
    ): Int? = transaction {

        setTenantSchema(tenantSchema)

        AcademicYearTable
            .selectAll()
            .where { AcademicYearTable.isCurrent eq true }
            .singleOrNull()
            ?.get(AcademicYearTable.id)
            ?.value
    }

    fun delete(
        tenantSchema: String,
        id: Int
    ): Boolean = transaction {

        setTenantSchema(tenantSchema)

        val reverseRules =
            NewClassPromotionTable
                .selectAll()
                .associate { row ->
                    val currentId =
                        row[NewClassPromotionTable.currentStageId].value

                    val nextId =
                        row[NewClassPromotionTable.nextStageId]?.value

                    nextId to currentId
                }
                .filterKeys { it != null }
                .mapKeys { it.key!! }

        val students = StudentsTable
            .selectAll()
            .map {
                Triple(
                    it[StudentsTable.id].value,
                    it[StudentsTable.currentNewGradeClass]?.value,
                    it[StudentsTable.user].value
                )
            }

        students.forEach { (studentId, currentStageId, _) ->

            val previousStageId = reverseRules[currentStageId]

            if (previousStageId != null) {

                StudentsTable.update({
                    StudentsTable.id eq studentId
                }) {
                    it[currentNewGradeClass] =
                        EntityID(previousStageId, NewGradeClassTable)

                    it[isGraduated] = false
                }
            }
        }

        AccountTable.update({
            AccountTable.isActive eq false
        }) {
            it[isActive] = true
        }

        AcademicYearTable.deleteWhere {
            AcademicYearTable.id eq id
        } > 0
    }

    fun findById(
        tenantSchema: String,
        id: Int
    ): AcademicYearModel? = transaction {

        setTenantSchema(tenantSchema)

        AcademicYearTable
            .selectAll()
            .where { AcademicYearTable.id eq id }
            .singleOrNull()
            ?.toAcademicYearModel()
    }

    fun patch(
        tenantSchema: String,
        id: Int,
        req: PatchAcademicYearRequest
    ): AcademicYearModel? = transaction {

        setTenantSchema(tenantSchema)

        val rowUpdated = AcademicYearTable.update(
            where = { AcademicYearTable.id eq id }
        ) { row ->
            req.name?.let {
                row[AcademicYearTable.name] = it
            }
        }

        if (rowUpdated == 0) {
            null
        } else {
            findById(tenantSchema, id)
        }
    }

    fun setCurrentAcademicYear(
        tenantSchema: String,
        id: Int
    ) = transaction {

        setTenantSchema(tenantSchema)

        AcademicYearTable.update({
            AcademicYearTable.isCurrent eq true
        }) {
            it[isCurrent] = false
        }

        AcademicYearTable.update({
            AcademicYearTable.id eq id
        }) {
            it[isCurrent] = true
        }
    }
}

