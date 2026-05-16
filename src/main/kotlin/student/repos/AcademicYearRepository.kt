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



        fun create(name: String): AcademicYearModel = transaction {
            val id = AcademicYearTable.insertAndGetId {
                it[AcademicYearTable.name] = name
            }.value

            // ✅ Equivalent to Django post_save(created=True)
            StudentPromotionRepository.promoteAllStudentsForNewAcademicYear()

            AcademicYearModel(
                id = id,
                name = name,
            )
        }



    fun findAll(): List<AcademicYearModel> = transaction {
        AcademicYearTable
            .selectAll()
            .orderBy(AcademicYearTable.id, SortOrder.DESC)
            .map {it.toAcademicYearModel()}
    }


    fun delete(id: Int): Boolean = transaction {

            // Reverse promotion map
            // Example:
            // class1 -> class2
            // becomes:
            // class2 -> class1

            val reverseRules: Map<Int, Int> =
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

            // Load students BEFORE updates
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

                    // Move student back
                    StudentsTable.update({
                        StudentsTable.id eq studentId
                    }) {
                        it[currentNewGradeClass] =
                            EntityID(previousStageId, NewGradeClassTable)

                        // Optional:
                        // if student had graduated before
                        it[isGraduated] = false
                    }
                }
            }

            // Reactivate all accounts
            AccountTable.update({
                AccountTable.isActive eq false
            }) {
                it[isActive] = true
            }

            // Finally delete academic year
            AcademicYearTable.deleteWhere {
                AcademicYearTable.id eq id
            } > 0
        }


    fun findById(id: Int): AcademicYearModel? = transaction {
        AcademicYearTable
            .selectAll()
            .where{ AcademicYearTable.id eq id }
            .singleOrNull()
        ?.toAcademicYearModel()
    }

    fun patch(id: Int, req: PatchAcademicYearRequest): AcademicYearModel? = transaction {
        val rowUpdated = AcademicYearTable.update(
            where = { AcademicYearTable.id eq id}
        ){ row ->
            req.name?.let {row[AcademicYearTable.name] = it }
        }

        if(rowUpdated == 0) {
            null
        }else {
            findById(id)!!
        }
        }
    }

