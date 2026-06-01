package com.example.academics.repos



import com.example.academics.dtos.response.StudentReportCardResponse
import com.example.academics.dtos.response.SubjectScoreInlineResponse
import com.example.academics.tables.*
import com.example.account.AccountTable
import com.example.minimals.*
import com.example.student.StudentsTable
import com.example.student.tables.AcademicYearTable
import com.example.student.tables.NewGradeClassTable
import com.example.student.tables.TermTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object StudentAcademicRecordRepository {

    /**
     * ✅ Shared base query (joins everything needed for report card + subjects)
     */
    private fun baseQuery(): Join {
        return AcademicRecordsTable
            .join(StudentsTable, JoinType.INNER, AcademicRecordsTable.student, StudentsTable.id)
            .join(AccountTable, JoinType.INNER, StudentsTable.user, AccountTable.id)
            .join(TermTable, JoinType.INNER, AcademicRecordsTable.term, TermTable.id)
            .join(AcademicYearTable, JoinType.INNER, AcademicRecordsTable.academicYear, AcademicYearTable.id)
            .join(NewGradeClassTable, JoinType.INNER, AcademicRecordsTable.classLevel, NewGradeClassTable.id)

            // subjects (prefetch)
            .join(SubjectScoresTable, JoinType.LEFT, AcademicRecordsTable.id, SubjectScoresTable.academicRecord)
            .join(SubjectsTable, JoinType.LEFT, SubjectScoresTable.subject, SubjectsTable.id)
            .join(GradesTable, JoinType.LEFT, SubjectScoresTable.grade, GradesTable.id)
    }

    /**
     * ✅ Equivalent of by_student(student_id)
     * GET all report cards for a student profile id
     */
    fun findAllByStudentId(studentId: Int): List<StudentReportCardResponse> = transaction {
        val studentEid = EntityID(studentId, StudentsTable)

        val rows = baseQuery()
            .selectAll()
            .where { AcademicRecordsTable.student eq studentEid }
            .orderBy(AcademicYearTable.id, SortOrder.ASC)
            .orderBy(TermTable.id, SortOrder.ASC)
            .toList()

        groupRowsToReportCards(rows)
    }

    /**
     * ✅ Equivalent of by_user(user_id) in Django
     * Here user_id is AccountTable.userId (string like "43227969")
     */
    fun findAllByUserId(userId: String): List<StudentReportCardResponse> = transaction {
        val rows = baseQuery()
            .selectAll()
            .where { AccountTable.userId eq userId }
            .orderBy(AcademicYearTable.id, SortOrder.ASC)
            .orderBy(TermTable.id, SortOrder.ASC)
            .toList()

        groupRowsToReportCards(rows)
    }

    /**
     * ✅ Equivalent of get_record(pk)
     */
    fun findOneByRecordId(recordId: Int): StudentReportCardResponse? = transaction {
        val rows = baseQuery()
            .selectAll()
            .where { AcademicRecordsTable.id eq recordId }
            .toList()

        groupRowsToReportCards(rows).firstOrNull()
    }

    /**
     * ✅ Convert joined rows => List<StudentReportCardResponse> (group by academic record id)
     */
    private fun groupRowsToReportCards(rows: List<ResultRow>): List<StudentReportCardResponse> {
        if (rows.isEmpty()) return emptyList()

        val grouped = rows.groupBy { it[AcademicRecordsTable.id].value }

        return grouped.values.map { groupRows ->
            val first = groupRows.first()

            val subjects = groupRows.mapNotNull { r ->
                val scoreId = r.getOrNull(SubjectScoresTable.id)?.value ?: return@mapNotNull null

                SubjectScoreInlineResponse(
                    id = scoreId,
                    subjectId = r[SubjectScoresTable.subject].value,
                    subjectName = r.getOrNull(SubjectsTable.name) ?: "Unknown",
                    classScore = r[SubjectScoresTable.classScore],
                    examScore = r[SubjectScoresTable.examScore],
                    totalScore = r[SubjectScoresTable.totalScore],
                    gradeId = r.getOrNull(GradesTable.id)?.value,
                    gradeCode = r.getOrNull(GradesTable.code),
                    gradeLabel = r.getOrNull(GradesTable.label),
                    interpretation = r[SubjectScoresTable.interpretation],
                    position = r[SubjectScoresTable.position]
                )
            }

            StudentReportCardResponse(
                id = first[AcademicRecordsTable.id].value,

                student = ComplexStudentMinimalDto(
                    id = first[StudentsTable.id].value,
                    name = first[AccountTable.fullName],
                    userId = first[AccountTable.userId],
                    profilePictureUrl = first[AccountTable.profilePictureUrl],
                    profilePicturePublicId = first[AccountTable.profilePicturePublicId]
                ),

                term = TermMinimal(

                    name = first[TermTable.name]
                ),

                academicYear = AcademicYearMinimal(

                    name = first[AcademicYearTable.name]
                ),

                classLevel = GradeClassMinimal(

                    name = first[NewGradeClassTable.name]
                ),

                overallPosition = first[AcademicRecordsTable.overallPosition],
                rawScoreTotal = first[AcademicRecordsTable.rawScoreTotal],
                promotedTo = first[AcademicRecordsTable.promotedTo],
                numberOnRoll = first[AcademicRecordsTable.numberOnRoll],
                attendance = first[AcademicRecordsTable.attendance],

                conduct = first[AcademicRecordsTable.conduct],
                interest = first[AcademicRecordsTable.interest],
                attitude = first[AcademicRecordsTable.attitude],
                teacherRemarks = first[AcademicRecordsTable.teacherRemarks],
                headTeacherRemarks = first[AcademicRecordsTable.headTeacherRemarks],
                nextTermBegins = first[AcademicRecordsTable.nextTermBegins],

                subjects = subjects
            )
        }
    }







}


