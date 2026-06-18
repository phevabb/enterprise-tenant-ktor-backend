package com.example.academics.tables



import com.example.student.StudentsTable
import com.example.student.tables.AcademicYearTable
import com.example.student.tables.NewGradeClassTable
import com.example.student.tables.TermTable
import org.jetbrains.exposed.dao.id.IntIdTable

object AcademicRecordsTable : IntIdTable("academic_records") {

    val overallPosition = integer("overall_position").nullable()

    val student = reference("student_id", StudentsTable)
    val term = reference("term_id", TermTable)
    val academicYear = reference("academic_year_id", AcademicYearTable)

    val classLevel = reference("class_level_id", NewGradeClassTable)

    val attendance = varchar("attendance", 20).nullable()

    val numberOnRoll = integer("number_on_roll").default(0)

    val conduct = varchar("conduct", 100).nullable()
    val interest = varchar("interest", 100).nullable()
    val attitude = varchar("attitude", 200).nullable()
    val teacherRemarks = varchar("teacher_remarks", 100).nullable()
    val headTeacherRemarks = varchar("head_teacher_remarks", 100).nullable()
    val nextTermBegins = varchar("next_term_begins", 100).nullable()
    val promotedTo = varchar("promoted_to", 100).nullable()

    val rawScoreTotal = integer("raw_score_total").nullable()

    // ✅ Django unique_together
    init {
        uniqueIndex(student, term, academicYear)
    }
}