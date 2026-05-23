package com.example.academics.tables




import org.jetbrains.exposed.dao.id.IntIdTable

object SubjectScoresTable : IntIdTable("subject_scores") {

    val position = integer("position").nullable()

    val academicRecord = reference("academic_record_id", AcademicRecordsTable)
    val subject = reference("subject_id", SubjectsTable)

    val classScore = integer("class_score").nullable()
    val examScore = integer("exam_score").nullable()

    val totalScore = integer("total_score").nullable()

    val grade = reference("grade_id", GradesTable).nullable()
    val interpretation = varchar("interpretation", 100).nullable()

    // ✅ unique_together
    init {
        uniqueIndex(academicRecord, subject)
    }
}