package com.example.academics.repos


import com.example.academics.mappers.toAcademicRecord
import com.example.academics.tables.AcademicRecordsTable



import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import com.example.academics.dtos.response.AcademicRecordDetailResponse
import com.example.academics.dtos.response.SubjectScoreInlineResponse

import com.example.academics.tables.SubjectScoresTable
import com.example.academics.tables.SubjectsTable
import com.example.academics.tables.GradesTable


import com.example.academics.dtos.response.AcademicRecordWithScoresResponse




object AcademicRecordRepository {

//    fun getOrCreate(
//        studentId: Int,
//        termId: Int,
//        academicYearId: Int,
//        classLevelId: Int
//    ): AcademicRecordResponse = transaction {
//
//        val existing = AcademicRecordsTable
//            .selectAll()
//            .where {
//                (AcademicRecordsTable.student eq studentId) and
//                        (AcademicRecordsTable.term eq termId) and
//                        (AcademicRecordsTable.academicYear eq academicYearId)
//            }
//            .singleOrNull()
//
//        if (existing != null) {
//            // ✅ Update class if changed
//            if (existing[AcademicRecordsTable.classLevel].value != classLevelId) {
//                AcademicRecordsTable.update(
//                    { AcademicRecordsTable.id eq existing[AcademicRecordsTable.id] }
//                ) {
//                    it[classLevel] = classLevelId
//                }
//            }
//            return@transaction existing.toAcademicRecord()
//        }
//
//        val newId = AcademicRecordsTable.insertAndGetId {
//            it[student] = studentId
//            it[term] = termId
//            it[academicYear] = academicYearId
//            it[classLevel] = classLevelId
//        }.value
//
//        AcademicRecordsTable
//            .selectAll()
//            .where { AcademicRecordsTable.id eq newId }
//            .single()
//            .toAcademicRecord()
//    }

    fun findAllWithScores(): List<AcademicRecordWithScoresResponse> = transaction {

        val rows = AcademicRecordsTable
            .join(SubjectScoresTable, JoinType.LEFT, AcademicRecordsTable.id, SubjectScoresTable.academicRecord)
            .join(SubjectsTable, JoinType.LEFT, SubjectScoresTable.subject, SubjectsTable.id)
            .join(GradesTable, JoinType.LEFT, SubjectScoresTable.grade, GradesTable.id)
            .selectAll()
            .orderBy(AcademicRecordsTable.id, SortOrder.DESC)
            .toList()

        if (rows.isEmpty()) return@transaction emptyList()

        // Group by AcademicRecord ID
        val grouped = rows.groupBy { it[AcademicRecordsTable.id].value }

        grouped.map { (_, groupRows) ->

            val first = groupRows.first()

            val subjectScores = groupRows.mapNotNull { r ->
                // If there is no subject score row (LEFT JOIN), SubjectScoresTable.id will be null
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
                studentId = first[AcademicRecordsTable.student].value,
                termId = first[AcademicRecordsTable.term].value,
                academicYearId = first[AcademicRecordsTable.academicYear].value,
                classLevelId = first[AcademicRecordsTable.classLevel].value,

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


    fun findByIdWithScores(recordId: Int): AcademicRecordDetailResponse? = transaction {

        val query = AcademicRecordsTable
            .join(SubjectScoresTable, JoinType.LEFT, AcademicRecordsTable.id, SubjectScoresTable.academicRecord)
            .join(SubjectsTable, JoinType.LEFT, SubjectScoresTable.subject, SubjectsTable.id)
            .join(GradesTable, JoinType.LEFT, SubjectScoresTable.grade, GradesTable.id)
            .selectAll()
            .where { AcademicRecordsTable.id eq recordId }

        val rows = query.toList()
        if (rows.isEmpty()) return@transaction null

        // Parent fields come from the first row
        val first = rows.first()

        val parent = AcademicRecordDetailResponse(
            id = first[AcademicRecordsTable.id].value,
            studentId = first[AcademicRecordsTable.student].value,
            termId = first[AcademicRecordsTable.term].value,
            academicYearId = first[AcademicRecordsTable.academicYear].value,
            classLevelId = first[AcademicRecordsTable.classLevel].value,

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

            subjectScores = rows.mapNotNull { r ->
                // If there is no SubjectScore row (LEFT JOIN), SubjectScoresTable.id will be null
                val ssId = r.getOrNull(SubjectScoresTable.id)?.value ?: return@mapNotNull null

                SubjectScoreInlineResponse(
                    id = ssId,
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
        )

        parent
    }



}









