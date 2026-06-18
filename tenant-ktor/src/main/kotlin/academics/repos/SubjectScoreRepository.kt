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
import com.example.academics.dtos.response.SubjectScoreContextResponse

object SubjectScoreRepository {

    fun createOrUpdate(
        tenantSchema: String,
        req: CreateOrUpdateSubjectScoreRequest
    ): SubjectScoreExpandedResponse = transaction {

        setTenantSchema(tenantSchema)

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

        // ✅ Check existing
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
                it[classScore] = req.classScore
                it[examScore] = req.examScore
                it[SubjectScoresTable.totalScore] = totalScore
                it[grade] = gradeEid
                it[SubjectScoresTable.interpretation] = interpretation
            }

            existingId
        } else {
            SubjectScoresTable.insertAndGetId {
                it[academicRecord] = recordEid
                it[subject] = subjectEid
                it[classScore] = req.classScore
                it[examScore] = req.examScore
                it[SubjectScoresTable.totalScore] = totalScore
                it[grade] = gradeEid
                it[SubjectScoresTable.interpretation] = interpretation
            }.value
        }

        findByIdExpanded(tenantSchema, scoreId)
            ?: throw IllegalStateException("Score saved but not found")
    }




    // ✅ GET ALL (expanded)
    fun findAllExpanded(
        tenantSchema: String
    ): List<SubjectScoreExpandedResponse> = transaction {

        setTenantSchema(tenantSchema)

        baseExpandedQuery()
            .selectAll()
            .orderBy(SubjectScoresTable.id, SortOrder.DESC)
            .map { it.toExpanded() }
    }


    // ✅ GET BY ID (expanded)
    fun findByIdExpanded(
        tenantSchema: String,
        id: Int
    ): SubjectScoreExpandedResponse? = transaction {

        setTenantSchema(tenantSchema)

        baseExpandedQuery()
            .selectAll()
            .where { SubjectScoresTable.id eq id }
            .singleOrNull()
            ?.toExpanded()
    }


    // ✅ GET BY ACADEMIC RECORD (expanded)
    fun findByAcademicRecordExpanded(
        tenantSchema: String,
        recordId: Int
    ): List<SubjectScoreExpandedResponse> = transaction {

        setTenantSchema(tenantSchema)

        baseExpandedQuery()
            .selectAll()
            .where { SubjectScoresTable.academicRecord eq recordId }
            .orderBy(SubjectsTable.name, SortOrder.ASC)
            .map { it.toExpanded() }
    }



    fun deleteById(
        tenantSchema: String,
        scoreId: Int
    ): Boolean = transaction {

        setTenantSchema(tenantSchema)

        SubjectScoresTable
            .deleteWhere { SubjectScoresTable.id eq scoreId } > 0
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

    fun findByContext(
        tenantSchema: String,
        classLevelId: Int,
        termId: Int,
        academicYearId: Int,
        subjectId: Int? = null
    ): List<SubjectScoreContextResponse> = transaction {
        setTenantSchema(tenantSchema)

        val classEid = EntityID(classLevelId, NewGradeClassTable)
        val termEid = EntityID(termId, TermTable)
        val yearEid = EntityID(academicYearId, AcademicYearTable)

        val base = SubjectScoresTable
            .join(AcademicRecordsTable, JoinType.INNER, SubjectScoresTable.academicRecord, AcademicRecordsTable.id)
            .join(StudentsTable, JoinType.INNER, AcademicRecordsTable.student, StudentsTable.id)
            .join(SubjectsTable, JoinType.INNER, SubjectScoresTable.subject, SubjectsTable.id)
            .join(GradesTable, JoinType.LEFT, SubjectScoresTable.grade, GradesTable.id)
            .selectAll()
            .where {
                (AcademicRecordsTable.classLevel eq classEid) and
                        (AcademicRecordsTable.term eq termEid) and
                        (AcademicRecordsTable.academicYear eq yearEid)
            }

        val filtered = if (subjectId != null) {
            base.andWhere { SubjectScoresTable.subject eq EntityID(subjectId, SubjectsTable) }
        } else base

        filtered.orderBy(StudentsTable.id, SortOrder.ASC).map { row ->
            SubjectScoreContextResponse(
                id = row[SubjectScoresTable.id].value,
                academicRecordId = row[SubjectScoresTable.academicRecord].value,
                studentId = row[StudentsTable.id].value,
                subjectId = row[SubjectsTable.id].value,
                subjectName = row[SubjectsTable.name],
                classScore = row[SubjectScoresTable.classScore],
                examScore = row[SubjectScoresTable.examScore],
                totalScore = row[SubjectScoresTable.totalScore],
                gradeCode = row.getOrNull(GradesTable.code),
                position = row[SubjectScoresTable.position]
            )
        }
    }


}










