package com.example.academics.services

import com.example.academics.tables.AcademicRecordsTable
import com.example.academics.tables.SubjectScoresTable
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.and

object RankingService {

    fun recomputeOverallPositions(
        classLevelId: Int,
        termId: Int,
        yearId: Int
    ) = transaction {

        val records = AcademicRecordsTable
            .selectAll()
            .where {
                (AcademicRecordsTable.classLevel eq classLevelId) and
                        (AcademicRecordsTable.term eq termId) and
                        (AcademicRecordsTable.academicYear eq yearId)
            }
            .map {
                Pair(
                    it[AcademicRecordsTable.id].value,
                    it[AcademicRecordsTable.rawScoreTotal] ?: 0
                )
            }
            .sortedByDescending { it.second }

        var position = 1

        for ((recordId, _) in records) {
            AcademicRecordsTable.update({ AcademicRecordsTable.id eq recordId }) {
                it[overallPosition] = position
            }
            position++
        }
    }

    fun recomputeSubjectPositions(
        classLevelId: Int,
        termId: Int,
        yearId: Int
    ) = transaction {

        // get all subjects involved
        val subjectIds = SubjectScoresTable
//            .slice(SubjectScoresTable.subject)
            .selectAll()
            .withDistinct()
            .map { it[SubjectScoresTable.subject].value }

        for (subjectId in subjectIds) {

            val rows = SubjectScoresTable
                .join(AcademicRecordsTable, JoinType.INNER)
                .selectAll()
                .where {
                    (SubjectScoresTable.subject eq subjectId) and
                            (AcademicRecordsTable.classLevel eq classLevelId) and
                            (AcademicRecordsTable.term eq termId) and
                            (AcademicRecordsTable.academicYear eq yearId)
                }
                .map {
                    Pair(
                        it[SubjectScoresTable.id].value,
                        it[SubjectScoresTable.totalScore] ?: 0
                    )
                }
                .sortedByDescending { it.second }

            var pos = 1

            for ((scoreId, _) in rows) {
                SubjectScoresTable.update({ SubjectScoresTable.id eq scoreId }) {
                    it[position] = pos
                }
                pos++
            }
        }
    }

    fun recomputeAll(recordId: Int) = transaction {

        val record = AcademicRecordsTable
            .selectAll()
            .where { AcademicRecordsTable.id eq recordId }
            .single()

        val classLevelId = record[AcademicRecordsTable.classLevel].value
        val termId = record[AcademicRecordsTable.term].value
        val yearId = record[AcademicRecordsTable.academicYear].value

        recomputeOverallPositions(classLevelId, termId, yearId)
        recomputeSubjectPositions(classLevelId, termId, yearId)
    }



}