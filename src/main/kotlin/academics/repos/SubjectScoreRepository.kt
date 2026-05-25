package com.example.academics.repos


import com.example.academics.dtos.requests.CreateOrUpdateSubjectScoreRequest
import com.example.academics.dtos.requests.PatchSubjectScoreRequest
import com.example.academics.dtos.response.SubjectScoreExpandedResponse
import com.example.academics.tables.AcademicRecordsTable
import com.example.academics.tables.GradesTable
import com.example.academics.tables.SubjectScoresTable
import com.example.academics.tables.SubjectsTable
import com.example.account.AccountTable
import com.example.minimals.*
import com.example.student.StudentsTable
import com.example.student.tables.AcademicYearTable
import com.example.student.tables.NewGradeClassTable
import com.example.student.tables.TermTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object SubjectScoreRepository {

    /**
     * ✅ CREATE / UPDATE (UPSERT) by (academicRecordId, subjectId)
     * - computes totalScore
     * - computes grade + interpretation
     * - saves into SubjectScoresTable
     * - returns EXPANDED response by scoreId
     *
     * NOTE: does NOT update AcademicRecordsTable.rawScoreTotal here — do it in the service.
     */
    fun createOrUpdate(req: CreateOrUpdateSubjectScoreRequest): SubjectScoreExpandedResponse = transaction {

        val recordEid = EntityID(req.academicRecordId, AcademicRecordsTable)
        val subjectEid = EntityID(req.subjectId, SubjectsTable)

        val classScoreSafe = req.classScore ?: 0
        val examScoreSafe = req.examScore ?: 0

        val totalScore: Int?
        val gradeEid: EntityID<Int>?
        val interpretation: String?

        if (classScoreSafe == 0 && examScoreSafe == 0) {
            totalScore = null
            gradeEid = null
            interpretation = null
        } else {
            val total = classScoreSafe + examScoreSafe
            totalScore = total

            val gradeRow = GradesTable
                .selectAll()
                .where {
                    (GradesTable.minScore lessEq total) and
                            (GradesTable.maxScore greaterEq total)
                }
                .orderBy(GradesTable.order to SortOrder.ASC)
                .limit(1)
                .singleOrNull()

            val gid = gradeRow?.get(GradesTable.id)?.value
            gradeEid = gid?.let { EntityID(it, GradesTable) }
            interpretation = gradeRow?.get(GradesTable.label)
        }

        // ✅ Check existing (uniqueIndex(academicRecord, subject))
        val existing = SubjectScoresTable
            .selectAll()
            .where {
                (SubjectScoresTable.academicRecord eq recordEid) and
                        (SubjectScoresTable.subject eq subjectEid)
            }
            .singleOrNull()

        val scoreId: Int = if (existing != null) {
            val existingId = existing[SubjectScoresTable.id].value

            SubjectScoresTable.update({ SubjectScoresTable.id eq existingId }) {
                it[SubjectScoresTable.classScore] = req.classScore
                it[SubjectScoresTable.examScore] = req.examScore
                it[SubjectScoresTable.totalScore] = totalScore
                it[SubjectScoresTable.grade] = gradeEid
                it[SubjectScoresTable.interpretation] = interpretation
            }

            existingId
        } else {
            SubjectScoresTable.insertAndGetId {
                it[academicRecord] = recordEid
                it[subject] = subjectEid
                it[SubjectScoresTable.classScore] = req.classScore
                it[SubjectScoresTable.examScore] = req.examScore
                it[SubjectScoresTable.totalScore] = totalScore
                it[SubjectScoresTable.grade] = gradeEid
                it[SubjectScoresTable.interpretation] = interpretation
            }.value
        }

        // ✅ Return expanded response
        findByIdExpanded(scoreId) ?: throw IllegalStateException("Score saved but not found")
    }

    // ✅ GET ALL (expanded)
    fun findAllExpanded(): List<SubjectScoreExpandedResponse> = transaction {
        baseExpandedQuery()
            .selectAll()
            .orderBy(SubjectScoresTable.id, SortOrder.DESC)
            .map { it.toExpanded() }
    }

    // ✅ GET BY ID (expanded)
    fun findByIdExpanded(id: Int): SubjectScoreExpandedResponse? = transaction {
        baseExpandedQuery()
            .selectAll()
            .where { SubjectScoresTable.id eq id }
            .singleOrNull()
            ?.toExpanded()
    }

    // ✅ GET BY ACADEMIC RECORD (expanded)
    fun findByAcademicRecordExpanded(recordId: Int): List<SubjectScoreExpandedResponse> = transaction {
        val recordEid = EntityID(recordId, AcademicRecordsTable)

        baseExpandedQuery()
            .selectAll()
            .where { SubjectScoresTable.academicRecord eq recordEid }
            .orderBy(SubjectsTable.name, SortOrder.ASC)
            .map { it.toExpanded() }
    }

    /**
     * ✅ Helper: get academicRecordId for a scoreId (NO slice)
     */
    fun getAcademicRecordIdByScoreId(scoreId: Int): Int? = transaction {
        SubjectScoresTable
            .selectAll()
            .where { SubjectScoresTable.id eq scoreId }
            .limit(1)
            .singleOrNull()
            ?.get(SubjectScoresTable.academicRecord)
            ?.value
    }

    /**
     * ✅ Helper: get subjectId for a scoreId (needed for PATCH recompute)
     */
    fun getSubjectIdByScoreId(scoreId: Int): Int? = transaction {
        SubjectScoresTable
            .selectAll()
            .where { SubjectScoresTable.id eq scoreId }
            .limit(1)
            .singleOrNull()
            ?.get(SubjectScoresTable.subject)
            ?.value
    }

    /**
     * ✅ PATCH raw fields (service should recompute totals/grade by calling createOrUpdate)
     */
    fun patchById(scoreId: Int, req: PatchSubjectScoreRequest): Boolean = transaction {
        SubjectScoresTable.update({ SubjectScoresTable.id eq scoreId }) { row ->
            req.classScore?.let { row[SubjectScoresTable.classScore] = it }
            req.examScore?.let { row[SubjectScoresTable.examScore] = it }
        } > 0
    }

    fun deleteById(scoreId: Int): Boolean = transaction {
        SubjectScoresTable.deleteWhere { SubjectScoresTable.id eq scoreId } > 0
    }

    // -------------------------------
    // Base join for expanded response
    // -------------------------------
    private fun baseExpandedQuery(): Join {
        return SubjectScoresTable
            .join(AcademicRecordsTable, JoinType.INNER, SubjectScoresTable.academicRecord, AcademicRecordsTable.id)
            .join(StudentsTable, JoinType.INNER, AcademicRecordsTable.student, StudentsTable.id)
            .join(AccountTable, JoinType.INNER, StudentsTable.user, AccountTable.id)
            .join(TermTable, JoinType.INNER, AcademicRecordsTable.term, TermTable.id)
            .join(AcademicYearTable, JoinType.INNER, AcademicRecordsTable.academicYear, AcademicYearTable.id)
            .join(NewGradeClassTable, JoinType.INNER, AcademicRecordsTable.classLevel, NewGradeClassTable.id)
            .join(SubjectsTable, JoinType.INNER, SubjectScoresTable.subject, SubjectsTable.id)
            .join(GradesTable, JoinType.LEFT, SubjectScoresTable.grade, GradesTable.id)
    }

    // -------------------------------
    // Mapper: ResultRow -> Expanded response
    // -------------------------------
    private fun ResultRow.toExpanded(): SubjectScoreExpandedResponse {

        // NOTE: This matches your current API shape (student has id + name, term/year/class have name only).
        val academicRecord = AcademicRecordMinimal(
            id = this[AcademicRecordsTable.id].value,
            overallPosition = this[AcademicRecordsTable.overallPosition],

            student = StudentMinimalDto(
                id = this[StudentsTable.id].value,
                name = this[AccountTable.fullName]
            ),

            term = TermMinimal(name = this[TermTable.name]),
            academicYear = AcademicYearMinimal(name = this[AcademicYearTable.name]),
            classLevel = GradeClassMinimal(name = this[NewGradeClassTable.name]),

            attendance = this[AcademicRecordsTable.attendance],
            numberOnRoll = this[AcademicRecordsTable.numberOnRoll],

            conduct = this[AcademicRecordsTable.conduct],
            interest = this[AcademicRecordsTable.interest],
            attitude = this[AcademicRecordsTable.attitude],
            teacherRemarks = this[AcademicRecordsTable.teacherRemarks],
            headTeacherRemarks = this[AcademicRecordsTable.headTeacherRemarks],
            nextTermBegins = this[AcademicRecordsTable.nextTermBegins],
            promotedTo = this[AcademicRecordsTable.promotedTo],

            rawScoreTotal = this[AcademicRecordsTable.rawScoreTotal]
        )

        return SubjectScoreExpandedResponse(
            id = this[SubjectScoresTable.id].value,
            academicRecord = academicRecord,

            subjectId = this[SubjectsTable.id].value,
            subjectName = this[SubjectsTable.name],

            classScore = this[SubjectScoresTable.classScore],
            examScore = this[SubjectScoresTable.examScore],
            totalScore = this[SubjectScoresTable.totalScore],

            gradeId = this.getOrNull(GradesTable.id)?.value,
            gradeCode = this.getOrNull(GradesTable.code),
            gradeLabel = this.getOrNull(GradesTable.label),

            interpretation = this[SubjectScoresTable.interpretation],
            position = this[SubjectScoresTable.position]
        )
    }
}

