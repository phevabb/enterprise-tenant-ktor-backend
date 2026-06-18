package com.example.academics.repos







import com.example.academics.dtos.response.ChartRecordResponse
import com.example.academics.dtos.response.ChartSubjectStat
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.math.round


import com.example.academics.tables.AcademicRecordsTable
import com.example.academics.tables.SubjectScoresTable
import com.example.academics.tables.SubjectsTable
import com.example.account.AccountTable
import com.example.student.StudentsTable
import com.example.student.tables.AcademicYearTable
import com.example.student.tables.NewGradeClassTable
import com.example.student.tables.TermTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.math.round

object PerformanceChartRepository {

    /**
     * ✅ Fetch ONE chart record for a student + term + year.
     *
     * studentId = StudentsTable.id (student_profile id)
     * termId    = TermTable.id
     * yearId    = AcademicYearTable.id
     */
    fun getStudentChart(tenantSchema: String,  studentId: Int, termId: Int, yearId: Int): ChartRecordResponse? = transaction {
        setTenantSchema(tenantSchema)
        val studentEid = EntityID(studentId, StudentsTable)
        val termEid = EntityID(termId, TermTable)
        val yearEid = EntityID(yearId, AcademicYearTable)

        // 1) Find the AcademicRecord for that student + term + year (unique)
        val recordRow = AcademicRecordsTable
            .join(StudentsTable, JoinType.INNER, AcademicRecordsTable.student, StudentsTable.id)
            .join(AccountTable, JoinType.INNER, StudentsTable.user, AccountTable.id)
            .join(NewGradeClassTable, JoinType.INNER, AcademicRecordsTable.classLevel, NewGradeClassTable.id)
            .selectAll()
            .where {
                (AcademicRecordsTable.student eq studentEid) and
                        (AcademicRecordsTable.term eq termEid) and
                        (AcademicRecordsTable.academicYear eq yearEid)
            }
            .singleOrNull()
            ?: return@transaction null

        val academicRecordId = recordRow[AcademicRecordsTable.id].value
        val classLevelId = recordRow[NewGradeClassTable.id].value

        val studentName = recordRow[AccountTable.fullName]
        val className = recordRow[NewGradeClassTable.name]
        val overallPos = recordRow[AcademicRecordsTable.overallPosition]
        val rawTotal = recordRow[AcademicRecordsTable.rawScoreTotal] ?: 0

        // 2) My scores (subject scores for THIS academic record)
        val myScoreRows = SubjectScoresTable
            .join(SubjectsTable, JoinType.INNER, SubjectScoresTable.subject, SubjectsTable.id)
            .selectAll()
            .where {
                SubjectScoresTable.academicRecord eq EntityID(academicRecordId, AcademicRecordsTable)
            }
            .toList()

        // If student has no scores yet, return empty subjects list (Django-like)
        if (myScoreRows.isEmpty()) {
            return@transaction ChartRecordResponse(
                student = studentName,
                `class` = className,
                position = overallPos,
                classAvg = rawTotal,
                subjects = emptyList()
            )
        }

        // 3) Class scores (ALL subject scores for this class + term + year)
        val classScoreRows = SubjectScoresTable
            .join(AcademicRecordsTable, JoinType.INNER, SubjectScoresTable.academicRecord, AcademicRecordsTable.id)
            .join(NewGradeClassTable, JoinType.INNER, AcademicRecordsTable.classLevel, NewGradeClassTable.id)
            .join(SubjectsTable, JoinType.INNER, SubjectScoresTable.subject, SubjectsTable.id)
            .selectAll()
            .where {
                (AcademicRecordsTable.classLevel eq EntityID(classLevelId, NewGradeClassTable)) and
                        (AcademicRecordsTable.term eq termEid) and
                        (AcademicRecordsTable.academicYear eq yearEid)
            }
            .toList()

        // 4) Compute stats per subject within this class (best, avg, worst)
        val statsBySubjectId: Map<Int, Stat> =
            classScoreRows
                .groupBy { it[SubjectsTable.id].value }
                .mapValues { (_, rows) ->
                    val scores: List<Int> = rows.map { it[SubjectScoresTable.totalScore] ?: 0 }

                    val best = scores.maxOrNull() ?: 0
                    val worst = scores.minOrNull() ?: 0
                    val avg = if (scores.isNotEmpty()) round(scores.average() * 10.0) / 10.0 else 0.0

                    Stat(best = best, avg = avg, worst = worst)
                }

        // 5) Build the student's subject output list
        val subjectsOut = myScoreRows.map { r ->
            val subjId = r[SubjectsTable.id].value
            val subjName = r[SubjectsTable.name]
            val score = r[SubjectScoresTable.totalScore] ?: 0

            val stat = statsBySubjectId[subjId] ?: Stat(best = 0, avg = 0.0, worst = 0)

            ChartSubjectStat(
                name = subjName.replaceFirstChar { ch ->
                    if (ch.isLowerCase()) ch.titlecase() else ch.toString()
                },
                score = score,
                best = stat.best,
                average = stat.avg,
                worst = stat.worst
            )
        }

        // 6) Final response (matches your Django serializer output)
        ChartRecordResponse(
            student = studentName,
            `class` = className,
            position = overallPos,
            classAvg = rawTotal,
            subjects = subjectsOut
        )
    }

    private data class Stat(val best: Int, val avg: Double, val worst: Int)
}
