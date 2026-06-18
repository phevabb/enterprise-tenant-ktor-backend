package com.example.academics.repos

import com.example.academics.tables.AcademicRecordsTable
import com.example.academics.tables.SubjectScoresTable
import org.jetbrains.exposed.dao.id.EntityID

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object AcademicRecordRepoLite {

    /**
     * ✅ Option 2 requirement:
     * Get or create AcademicRecord for (student, term, academicYear).
     * If record exists but classLevel differs, update classLevel.
     *
     * Returns recordId (Int).
     */
    fun getOrCreate(
        studentId: Int,
        termId: Int,
        yearId: Int,
        classLevelId: Int
    ): Int = transaction {

        val existing = AcademicRecordsTable
            .selectAll()
            .where {
                (AcademicRecordsTable.student eq studentId) and
                        (AcademicRecordsTable.term eq termId) and
                        (AcademicRecordsTable.academicYear eq yearId)
            }
            .singleOrNull()

        if (existing != null) {
            val recordId = existing[AcademicRecordsTable.id].value

            // ✅ If class changed, update classLevel (Django behavior)
            val oldClassLevelId = existing[AcademicRecordsTable.classLevel].value
            if (oldClassLevelId != classLevelId) {
                AcademicRecordsTable.update({ AcademicRecordsTable.id eq recordId }) {
                    it[AcademicRecordsTable.classLevel] = EntityID(classLevelId, com.example.student.tables.NewGradeClassTable)
                }
            }

            return@transaction recordId
        }

        // ✅ Create new record
        AcademicRecordsTable.insertAndGetId {
            it[AcademicRecordsTable.student] = EntityID(studentId, com.example.student.StudentsTable)
            it[AcademicRecordsTable.term] = EntityID(termId, com.example.student.tables.TermTable)
            it[AcademicRecordsTable.academicYear] = EntityID(yearId, com.example.student.tables.AcademicYearTable)
            it[AcademicRecordsTable.classLevel] = EntityID(classLevelId, com.example.student.tables.NewGradeClassTable)
        }.value
    }

    /**
     * ✅ Recompute rawScoreTotal after any score create/update/delete.
     * NO .slice() used.
     * If there are no non-null totals, rawScoreTotal becomes null (Django-like).
     */
    fun recomputeRawTotal(recordId: Int) = transaction {

        val totals: List<Int> = SubjectScoresTable
            .selectAll()
            .where { SubjectScoresTable.academicRecord eq EntityID(recordId, AcademicRecordsTable) }
            .mapNotNull { it[SubjectScoresTable.totalScore] }

        val sumTotal: Int? = totals.sum().takeIf { totals.isNotEmpty() }

        AcademicRecordsTable.update({ AcademicRecordsTable.id eq recordId }) {
            it[AcademicRecordsTable.rawScoreTotal] = sumTotal
        }
    }


}

