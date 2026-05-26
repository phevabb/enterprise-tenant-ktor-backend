package com.example.academics.services

import com.example.academics.dtos.requests.CreateOrUpdateSubjectScoreRequest

import com.example.academics.dtos.response.SubjectScoreExpandedResponse
import com.example.academics.repos.AcademicRecordRepoLite
import com.example.academics.repos.SubjectScoreRepository
import org.jetbrains.exposed.sql.transactions.transaction






import com.example.academics.dtos.requests.CreateSubjectScoreByStudentRequest

import com.example.academics.repos.SubjectRepoLite

import com.example.student.repos.AcademicYearRepository
import com.example.student.repos.StudentLiteRepo
import com.example.student.repos.TermRepository

import com.example.academics.dtos.requests.PatchSubjectScoreRequest



object SubjectScoreService {

    /**
     * ✅ Option 1: Upsert using academicRecordId + subjectId
     */
    fun createOrUpdate(req: CreateOrUpdateSubjectScoreRequest): SubjectScoreExpandedResponse = transaction {

        val saved = SubjectScoreRepository.createOrUpdate(req)

        AcademicRecordRepoLite.recomputeRawTotal(req.academicRecordId)
        RankingService.recomputeAll(req.academicRecordId)

        SubjectScoreRepository.findByIdExpanded(saved.id)
            ?: throw IllegalStateException("Score saved but not found")
    }

    /**
     * ✅ Option 2: Teacher flow (student + subject + scores)
     * Auto resolves current term/year, student class, get-or-create record, then upsert.
     */
    fun createOrUpdateByStudent(req: CreateSubjectScoreByStudentRequest): SubjectScoreExpandedResponse = transaction {

        val studentId = req.student

        val subjectId = SubjectRepoLite.findIdByIdOrName(req.subject)
            ?: throw IllegalArgumentException("Invalid subject")

        val (termId, yearId) = TermRepository.getCurrent()
            ?: throw IllegalArgumentException("No term found in database")

        val classLevelId = StudentLiteRepo.getStudentClassLevelId(studentId)
            ?: throw IllegalArgumentException("Student has no assigned class")

        val recordId = AcademicRecordRepoLite.getOrCreate(
            studentId = studentId,
            termId = termId,
            yearId = yearId,
            classLevelId = classLevelId
        )

        val saved = SubjectScoreRepository.createOrUpdate(
            CreateOrUpdateSubjectScoreRequest(
                academicRecordId = recordId,
                subjectId = subjectId,
                classScore = req.classScore,
                examScore = req.examScore
            )
        )

        AcademicRecordRepoLite.recomputeRawTotal(recordId)
        RankingService.recomputeAll(recordId)

        SubjectScoreRepository.findByIdExpanded(saved.id)
            ?: throw IllegalStateException("Score saved but not found")
    }

    /**
     * ✅ PATCH by scoreId (recomputes totals/grade by reusing createOrUpdate)
     */
    fun patch(scoreId: Int, req: PatchSubjectScoreRequest): SubjectScoreExpandedResponse = transaction {

        val existing = SubjectScoreRepository.findByIdExpanded(scoreId)
            ?: throw IllegalArgumentException("Subject score not found")

        val recordId = existing.academicRecord.id
        val subjectId = existing.subjectId

        val newClassScore = req.classScore ?: existing.classScore
        val newExamScore = req.examScore ?: existing.examScore

        val updated = createOrUpdate(
            CreateOrUpdateSubjectScoreRequest(
                academicRecordId = recordId,
                subjectId = subjectId,
                classScore = newClassScore,
                examScore = newExamScore
            )
        )

        // createOrUpdate already recomputed raw total using recordId, but it used req.academicRecordId above.
        // Here recordId is correct; createOrUpdate recomputed with that id.
        updated
    }

    /**
     * ✅ DELETE by scoreId
     */
    fun delete(scoreId: Int) = transaction {

        val existing = SubjectScoreRepository.findByIdExpanded(scoreId)
            ?: throw IllegalArgumentException("Subject score not found")

        val recordId = existing.academicRecord.id

        val ok = SubjectScoreRepository.deleteById(scoreId)
        if (!ok) throw IllegalArgumentException("Subject score not found")

        AcademicRecordRepoLite.recomputeRawTotal(recordId)
        RankingService.recomputeAll(recordId)
    }
}