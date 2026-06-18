package com.example.academics.mappers

import com.example.academics.models.SubjectScore
import com.example.academics.tables.SubjectScoresTable

import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toSubjectScore() = SubjectScore(
    id = this[SubjectScoresTable.id].value,

    academicRecordId = this[SubjectScoresTable.academicRecord].value,
    subjectId = this[SubjectScoresTable.subject].value,

    position = this[SubjectScoresTable.position],

    classScore = this[SubjectScoresTable.classScore],
    examScore = this[SubjectScoresTable.examScore],
    totalScore = this[SubjectScoresTable.totalScore],

    gradeId = this[SubjectScoresTable.grade]?.value,
    interpretation = this[SubjectScoresTable.interpretation]
)