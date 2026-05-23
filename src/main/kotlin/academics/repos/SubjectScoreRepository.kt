package com.example.academics.repos



import com.example.academics.dtos.requests.CreateSubjectScoreRequest
import com.example.academics.mappers.toSubjectScore
import com.example.academics.models.SubjectScore
import com.example.academics.tables.GradesTable
import com.example.academics.tables.SubjectScoresTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.between
import org.jetbrains.exposed.sql.transactions.transaction

object SubjectScoreRepository {

    fun createOrUpdate(req: CreateSubjectScoreRequest): SubjectScore = transaction {

        val classScore = req.classScore ?: 0
        val examScore = req.examScore ?: 0

        val totalScore: Int?
        val gradeId: Int?
        val interpretation: String?

        if (classScore == 0 && examScore == 0) {
            totalScore = null
            gradeId = null
            interpretation = null
        } else {
            val total = classScore + examScore
            totalScore = total

            val gradeRow = GradesTable
                .selectAll()

                .where {
                    (GradesTable.minScore lessEq total) and
                            (GradesTable.maxScore greaterEq total)

                }

                .limit(1)
                .singleOrNull()

            gradeId = gradeRow?.get(GradesTable.id)?.value
            interpretation = gradeRow?.get(GradesTable.label)
        }

        // ✅ Check existing (unique constraint)
        val existing = SubjectScoresTable.selectAll().where {
            (SubjectScoresTable.academicRecord eq req.academicRecordId) and
                    (SubjectScoresTable.subject eq req.subjectId)
        }.singleOrNull()

        if (existing != null) {

            SubjectScoresTable.update(
                { SubjectScoresTable.id eq existing[SubjectScoresTable.id] }
            ) {
                it[SubjectScoresTable.classScore] = req.classScore
                it[SubjectScoresTable.examScore] = req.examScore
                it[SubjectScoresTable.totalScore] = totalScore
                it[grade] = gradeId
                it[SubjectScoresTable.interpretation] = interpretation
            }

            return@transaction existing.toSubjectScore()
        }

        val newId = SubjectScoresTable.insertAndGetId {
            it[academicRecord] = req.academicRecordId
            it[subject] = req.subjectId
            it[SubjectScoresTable.classScore] = req.classScore
            it[SubjectScoresTable.examScore] = req.examScore
            it[SubjectScoresTable.totalScore] = totalScore
            it[grade] = gradeId
            it[SubjectScoresTable.interpretation] = interpretation
        }.value

        SubjectScoresTable
            .selectAll()
            .where { SubjectScoresTable.id eq newId }
            .single()
            .toSubjectScore()
    }

    fun findByAcademicRecord(recordId: Int): List<SubjectScore> = transaction {
        SubjectScoresTable
            .selectAll()
            .where { SubjectScoresTable.academicRecord eq recordId }
            .map { it.toSubjectScore() }
    }
}