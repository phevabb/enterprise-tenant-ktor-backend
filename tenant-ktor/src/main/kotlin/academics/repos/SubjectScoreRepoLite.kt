package com.example.academics.repos

import com.example.academics.dtos.response.SubjectDetailResponse
import com.example.academics.dtos.response.SubjectScoreResponse
import com.example.academics.tables.SubjectScoresTable
import com.example.academics.tables.SubjectsTable
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.and

object SubjectScoreRepoLite {

    fun upsert(
        academicRecordId: Int,
        subjectId: Int,
        classScore: Int?,
        examScore: Int?
    ): Int = transaction {

        val existing = SubjectScoresTable.selectAll()
            .where {
                (SubjectScoresTable.academicRecord eq academicRecordId) and
                        (SubjectScoresTable.subject eq subjectId)
            }
            .singleOrNull()

        // compute totals/grade like Django model.save()
        val c = classScore ?: 0
        val e = examScore ?: 0

        val totalScore: Int?
        val gradeId: Int?
        val interpretation: String?

        if (c == 0 && e == 0) {
            totalScore = null
            gradeId = null
            interpretation = null
        } else {
            val total = c + e
            totalScore = total
            val grade = GradeRepoLite.findGradeByScore(total)
            gradeId = grade?.first
            interpretation = grade?.third
        }

        if (existing != null) {
            val scoreId = existing[SubjectScoresTable.id].value

            SubjectScoresTable.update({ SubjectScoresTable.id eq scoreId }) {
                it[SubjectScoresTable.classScore] = classScore
                it[SubjectScoresTable.examScore] = examScore
                it[SubjectScoresTable.totalScore] = totalScore
                it[SubjectScoresTable.grade] = gradeId
                it[SubjectScoresTable.interpretation] = interpretation
            }

            return@transaction scoreId
        }

        SubjectScoresTable.insertAndGetId {
            it[academicRecord] = academicRecordId
            it[subject] = subjectId
            it[SubjectScoresTable.classScore] = classScore
            it[SubjectScoresTable.examScore] = examScore
            it[SubjectScoresTable.totalScore] = totalScore
            it[SubjectScoresTable.grade] = gradeId
            it[SubjectScoresTable.interpretation] = interpretation
        }.value
    }

    fun findResponseById(scoreId: Int): SubjectScoreResponse? = transaction {
        val row = SubjectScoresTable
            .join(SubjectsTable, JoinType.INNER, SubjectScoresTable.subject, SubjectsTable.id)
            .selectAll()
            .where { SubjectScoresTable.id eq scoreId }
            .singleOrNull() ?: return@transaction null

        SubjectScoreResponse(
            id = row[SubjectScoresTable.id].value,
            academic_record = row[SubjectScoresTable.academicRecord].value,
            subject = row[SubjectScoresTable.subject].value,
            subject_detail = SubjectDetailResponse(
                id = row[SubjectsTable.id].value,
                name = row[SubjectsTable.name]
            ),
            class_score = row[SubjectScoresTable.classScore],
            exam_score = row[SubjectScoresTable.examScore],
            total_score = row[SubjectScoresTable.totalScore],
            grade = row[SubjectScoresTable.grade]?.value,
            interpretation = row[SubjectScoresTable.interpretation],
            position = row[SubjectScoresTable.position]
        )
    }
}