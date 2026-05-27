package com.example.academics.repos

import com.example.academics.dtos.requests.NewCreateAcademicRecordRequest
import com.example.academics.dtos.requests.PatchAcademicRecordRemarksRequest
import com.example.academics.tables.*
import com.example.academics.dtos.response.*
import com.example.account.AccountTable
import com.example.minimals.*
import com.example.student.StudentsTable
import com.example.student.repos.TermRepository
import com.example.student.tables.AcademicYearTable
import com.example.student.tables.NewGradeClassTable
import com.example.student.tables.TermTable
import org.jetbrains.exposed.dao.id.EntityID

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object AcademicRecordRepository {

    fun findAllWithScores(): List<AcademicRecordWithScoresResponse> = transaction {

        val rows = AcademicRecordsTable

            // ✅ FIXED: REQUIRED JOINS
            .join(StudentsTable, JoinType.INNER, AcademicRecordsTable.student, StudentsTable.id)
            .join(AccountTable, JoinType.INNER, StudentsTable.user, AccountTable.id)
            .join(TermTable, JoinType.INNER, AcademicRecordsTable.term, TermTable.id)
            .join(AcademicYearTable, JoinType.INNER, AcademicRecordsTable.academicYear, AcademicYearTable.id)
            .join(NewGradeClassTable, JoinType.INNER, AcademicRecordsTable.classLevel, NewGradeClassTable.id)

            // ✅ LEFT JOIN for scores
            .join(SubjectScoresTable, JoinType.LEFT, AcademicRecordsTable.id, SubjectScoresTable.academicRecord)
            .join(SubjectsTable, JoinType.LEFT, SubjectScoresTable.subject, SubjectsTable.id)
            .join(GradesTable, JoinType.LEFT, SubjectScoresTable.grade, GradesTable.id)

            .selectAll()
            .orderBy(AcademicRecordsTable.id, SortOrder.DESC)
            .toList()

        if (rows.isEmpty()) return@transaction emptyList()

        val grouped = rows.groupBy { it[AcademicRecordsTable.id].value }

        grouped.map { (_, groupRows) ->

            val first = groupRows.first()

            val subjectScores = groupRows.mapNotNull { r ->
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

            AcademicRecordWithScoresResponse(
                id = first[AcademicRecordsTable.id].value,

                // ✅ FIXED: Nested Student DTO
                student = ComplexStudentMinimalDto(
                    id = first[StudentsTable.id].value,
                    name = first[AccountTable.fullName],
                    userId = first[AccountTable.userId]
                ),

                // ✅ FIXED: Term DTO
                term = TermMinimal(

                    name = first[TermTable.name]
                ),

                // ✅ FIXED: AcademicYear DTO
                academicYear = AcademicYearMinimal(

                    name = first[AcademicYearTable.name]
                ),

                gradeClass = GradeClassMinimal(
                    name = first[NewGradeClassTable.name]

                ),

                overallPosition = first[AcademicRecordsTable.overallPosition],
                attendance = first[AcademicRecordsTable.attendance],
                numberOnRoll = first[AcademicRecordsTable.numberOnRoll],

                conduct = first[AcademicRecordsTable.conduct],
                interest = first[AcademicRecordsTable.interest],
                attitude = first[AcademicRecordsTable.attitude],
                teacherRemarks = first[AcademicRecordsTable.teacherRemarks],
                headTeacherRemarks = first[AcademicRecordsTable.headTeacherRemarks],
                nextTermBegins = first[AcademicRecordsTable.nextTermBegins],
                promotedTo = first[AcademicRecordsTable.promotedTo],

                rawScoreTotal = first[AcademicRecordsTable.rawScoreTotal],

                subjectScores = subjectScores
            )
        }
    }

    fun findByIdWithScores(recordId: Int): AcademicRecordWithScoresResponse? = transaction {

        val rows = AcademicRecordsTable

            // ✅ REQUIRED JOINS
            .join(StudentsTable, JoinType.INNER, AcademicRecordsTable.student, StudentsTable.id)
            .join(AccountTable, JoinType.INNER, StudentsTable.user, AccountTable.id)
            .join(TermTable, JoinType.INNER, AcademicRecordsTable.term, TermTable.id)
            .join(AcademicYearTable, JoinType.INNER, AcademicRecordsTable.academicYear, AcademicYearTable.id)
            .join(NewGradeClassTable, JoinType.INNER, AcademicRecordsTable.classLevel, NewGradeClassTable.id)

            .join(SubjectScoresTable, JoinType.LEFT, AcademicRecordsTable.id, SubjectScoresTable.academicRecord)
            .join(SubjectsTable, JoinType.LEFT, SubjectScoresTable.subject, SubjectsTable.id)
            .join(GradesTable, JoinType.LEFT, SubjectScoresTable.grade, GradesTable.id)

            .selectAll()
            .where { AcademicRecordsTable.id eq recordId }
            .toList()

        if (rows.isEmpty()) return@transaction null

        val first = rows.first()

        // ✅ FIX: build subjectScores list
        val subjectScores = rows.mapNotNull { r ->
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

        AcademicRecordWithScoresResponse(
            id = first[AcademicRecordsTable.id].value,

            student = ComplexStudentMinimalDto(
                id = first[StudentsTable.id].value,
                name = first[AccountTable.fullName],
                userId = first[AccountTable.userId]
            ),

            term = TermMinimal(

                name = first[TermTable.name]
            ),

            academicYear = AcademicYearMinimal(

                name = first[AcademicYearTable.name]
            ),

            gradeClass = GradeClassMinimal(
                name = first[NewGradeClassTable.name]

                    ),

            overallPosition = first[AcademicRecordsTable.overallPosition],
            attendance = first[AcademicRecordsTable.attendance],
            numberOnRoll = first[AcademicRecordsTable.numberOnRoll],

            conduct = first[AcademicRecordsTable.conduct],
            interest = first[AcademicRecordsTable.interest],
            attitude = first[AcademicRecordsTable.attitude],
            teacherRemarks = first[AcademicRecordsTable.teacherRemarks],
            headTeacherRemarks = first[AcademicRecordsTable.headTeacherRemarks],
            nextTermBegins = first[AcademicRecordsTable.nextTermBegins],
            promotedTo = first[AcademicRecordsTable.promotedTo],

            rawScoreTotal = first[AcademicRecordsTable.rawScoreTotal],

            subjectScores = subjectScores
        )
    }

    fun updateRemarks(
        id: Int,
        req: PatchAcademicRecordRemarksRequest
    ): AcademicRecordWithScoresResponse = transaction {

        AcademicRecordsTable.update({ AcademicRecordsTable.id eq id }) {

            req.attendance?.let { v -> it[attendance] = v }
            req.numberOnRoll?.let { v -> it[numberOnRoll] = v }
            req.promotedTo?.let { v -> it[promotedTo] = v }

            req.conduct?.let { v -> it[conduct] = v }
            req.interest?.let { v -> it[interest] = v }
            req.attitude?.let { v -> it[attitude] = v }

            req.teacherRemarks?.let { v -> it[teacherRemarks] = v }
            req.headTeacherRemarks?.let { v -> it[headTeacherRemarks] = v }
            req.nextTermBegins?.let { v -> it[nextTermBegins] = v }
        }

        findByIdWithScores(id)
            ?: throw IllegalStateException("Updated but not found")
    }

    fun findByClass(classId: Int): List<AcademicRecordWithScoresResponse> = transaction {

        val rows = AcademicRecordsTable

            .join(StudentsTable, JoinType.INNER, AcademicRecordsTable.student, StudentsTable.id)
            .join(AccountTable, JoinType.INNER, StudentsTable.user, AccountTable.id)
            .join(TermTable, JoinType.INNER, AcademicRecordsTable.term, TermTable.id)
            .join(AcademicYearTable, JoinType.INNER, AcademicRecordsTable.academicYear, AcademicYearTable.id)
            .join(NewGradeClassTable, JoinType.INNER, AcademicRecordsTable.classLevel, NewGradeClassTable.id)

            .join(SubjectScoresTable, JoinType.LEFT, AcademicRecordsTable.id, SubjectScoresTable.academicRecord)
            .join(SubjectsTable, JoinType.LEFT, SubjectScoresTable.subject, SubjectsTable.id)
            .join(GradesTable, JoinType.LEFT, SubjectScoresTable.grade, GradesTable.id)

            .selectAll()
            .where { AcademicRecordsTable.classLevel eq classId }
            .orderBy(AccountTable.fullName, SortOrder.ASC)
            .toList()

        if (rows.isEmpty()) return@transaction emptyList()

        val grouped = rows.groupBy { it[AcademicRecordsTable.id].value }

        grouped.map { (_, groupRows) ->

            val first = groupRows.first()

            val subjectScores = groupRows.mapNotNull { r ->
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

            AcademicRecordWithScoresResponse(
                id = first[AcademicRecordsTable.id].value,

                student = ComplexStudentMinimalDto(
                    id = first[StudentsTable.id].value,
                    name = first[AccountTable.fullName],
                    userId = first[AccountTable.userId]
                ),

                term = TermMinimal(

                    name = first[TermTable.name]
                ),

                academicYear = AcademicYearMinimal(

                    name = first[AcademicYearTable.name]
                ),

                gradeClass = GradeClassMinimal(

                    name = first[NewGradeClassTable.name]
                ),



                overallPosition = first[AcademicRecordsTable.overallPosition],
                attendance = first[AcademicRecordsTable.attendance],
                numberOnRoll = first[AcademicRecordsTable.numberOnRoll],

                conduct = first[AcademicRecordsTable.conduct],
                interest = first[AcademicRecordsTable.interest],
                attitude = first[AcademicRecordsTable.attitude],
                teacherRemarks = first[AcademicRecordsTable.teacherRemarks],
                headTeacherRemarks = first[AcademicRecordsTable.headTeacherRemarks],
                nextTermBegins = first[AcademicRecordsTable.nextTermBegins],
                promotedTo = first[AcademicRecordsTable.promotedTo],

                rawScoreTotal = first[AcademicRecordsTable.rawScoreTotal],
                subjectScores = subjectScores
            )
        }
    }


    fun deleteById(recordId: Int): Boolean = transaction {
        // ✅ Delete children first to avoid FK constraint issues
        SubjectScoresTable.deleteWhere { SubjectScoresTable.academicRecord eq recordId }

        // ✅ Delete the parent record
        AcademicRecordsTable.deleteWhere { AcademicRecordsTable.id eq recordId } > 0
    }



    fun findByClassCurrent(classId: Int): List<AcademicRecordWithScoresResponse> = transaction {

        // ✅ current term + its academic year (your rule: latest term id)
        val current = TermRepository.getCurrent_()
            ?: return@transaction emptyList()

        val currentTermId = current.id
        val currentYearId = current.academic_year.id

        val rows = AcademicRecordsTable

            .join(StudentsTable, JoinType.INNER, AcademicRecordsTable.student, StudentsTable.id)
            .join(AccountTable, JoinType.INNER, StudentsTable.user, AccountTable.id)
            .join(TermTable, JoinType.INNER, AcademicRecordsTable.term, TermTable.id)
            .join(AcademicYearTable, JoinType.INNER, AcademicRecordsTable.academicYear, AcademicYearTable.id)
            .join(NewGradeClassTable, JoinType.INNER, AcademicRecordsTable.classLevel, NewGradeClassTable.id)

            .join(SubjectScoresTable, JoinType.LEFT, AcademicRecordsTable.id, SubjectScoresTable.academicRecord)
            .join(SubjectsTable, JoinType.LEFT, SubjectScoresTable.subject, SubjectsTable.id)
            .join(GradesTable, JoinType.LEFT, SubjectScoresTable.grade, GradesTable.id)

            .selectAll()
            .where {
                (AcademicRecordsTable.classLevel eq EntityID(classId, NewGradeClassTable)) and
                        (AcademicRecordsTable.term eq EntityID(currentTermId, TermTable)) and
                        (AcademicRecordsTable.academicYear eq EntityID(currentYearId, AcademicYearTable))
            }
            .orderBy(AccountTable.fullName, SortOrder.ASC)
            .toList()

        if (rows.isEmpty()) return@transaction emptyList()

        val grouped = rows.groupBy { it[AcademicRecordsTable.id].value }

        grouped.map { (_, groupRows) ->

            val first = groupRows.first()

            val subjectScores = groupRows.mapNotNull { r ->
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

            AcademicRecordWithScoresResponse(
                id = first[AcademicRecordsTable.id].value,

                student = ComplexStudentMinimalDto(
                    id = first[StudentsTable.id].value,
                    name = first[AccountTable.fullName],
                    userId = first[AccountTable.userId]
                ),

                term = TermMinimal(
                    name = first[TermTable.name]
                ),

                academicYear = AcademicYearMinimal(
                    name = first[AcademicYearTable.name]
                ),

                gradeClass = GradeClassMinimal(
                    name = first[NewGradeClassTable.name]
                ),

                overallPosition = first[AcademicRecordsTable.overallPosition],
                attendance = first[AcademicRecordsTable.attendance],
                numberOnRoll = first[AcademicRecordsTable.numberOnRoll],

                conduct = first[AcademicRecordsTable.conduct],
                interest = first[AcademicRecordsTable.interest],
                attitude = first[AcademicRecordsTable.attitude],
                teacherRemarks = first[AcademicRecordsTable.teacherRemarks],
                headTeacherRemarks = first[AcademicRecordsTable.headTeacherRemarks],
                nextTermBegins = first[AcademicRecordsTable.nextTermBegins],
                promotedTo = first[AcademicRecordsTable.promotedTo],

                rawScoreTotal = first[AcademicRecordsTable.rawScoreTotal],
                subjectScores = subjectScores
            )
        }
    }


}







