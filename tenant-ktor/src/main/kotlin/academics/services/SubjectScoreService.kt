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
import com.example.academics.repos.setTenantSchema


object SubjectScoreService {

    fun createOrUpdate(
        tenantSchema: String,
        req: CreateOrUpdateSubjectScoreRequest
    ): SubjectScoreExpandedResponse = transaction {

        setTenantSchema(tenantSchema)

        val saved = SubjectScoreRepository.createOrUpdate(
            tenantSchema = tenantSchema,
            req = req
        )

        AcademicRecordRepoLite.recomputeRawTotal(req.academicRecordId)
        RankingService.recomputeAll(req.academicRecordId)

        SubjectScoreRepository.findByIdExpanded(
            tenantSchema = tenantSchema,
            id = saved.id
        ) ?: throw IllegalStateException("Score saved but not found")
    }


    fun createOrUpdateByStudent(
        tenantSchema: String,
        req: CreateSubjectScoreByStudentRequest
    ): SubjectScoreExpandedResponse = transaction {

        setTenantSchema(tenantSchema)

        val studentId = req.student

        val subjectId = SubjectRepoLite.findIdByIdOrName(tenantSchema, req.subject)
            ?: throw IllegalArgumentException("Invalid subject")

        val (termId, yearId) = TermRepository.getCurrent(tenantSchema)
            ?: throw IllegalArgumentException("No term found in database")

        val classLevelId = StudentLiteRepo.getStudentClassLevelId( tenantSchema, studentId)
            ?: throw IllegalArgumentException("Student has no assigned class")

        val recordId = AcademicRecordRepoLite.getOrCreate(
            studentId = studentId,
            termId = termId,
            yearId = yearId,
            classLevelId = classLevelId
        )

        val saved = SubjectScoreRepository.createOrUpdate(
            tenantSchema = tenantSchema,
            req = CreateOrUpdateSubjectScoreRequest(
                academicRecordId = recordId,
                subjectId = subjectId,
                classScore = req.classScore,
                examScore = req.examScore
            )
        )

        AcademicRecordRepoLite.recomputeRawTotal(recordId)
        RankingService.recomputeAll(recordId)

        SubjectScoreRepository.findByIdExpanded(
            tenantSchema = tenantSchema,
            id = saved.id
        ) ?: throw IllegalStateException("Score saved but not found")
    }

    fun patch(
        tenantSchema: String,
        scoreId: Int,
        req: PatchSubjectScoreRequest
    ): SubjectScoreExpandedResponse = transaction {

        setTenantSchema(tenantSchema)

        val existing = SubjectScoreRepository.findByIdExpanded(
            tenantSchema = tenantSchema,
            id = scoreId
        ) ?: throw IllegalArgumentException("Subject score not found")

        val recordId = existing.academicRecord.id
        val subjectId = existing.subjectId

        val newClassScore = req.classScore ?: existing.classScore
        val newExamScore = req.examScore ?: existing.examScore

        createOrUpdate(
            tenantSchema = tenantSchema,
            req = CreateOrUpdateSubjectScoreRequest(
                academicRecordId = recordId,
                subjectId = subjectId,
                classScore = newClassScore,
                examScore = newExamScore
            )
        )
    }

    fun delete(
        tenantSchema: String,
        scoreId: Int
    ) = transaction {

        setTenantSchema(tenantSchema)

        val existing = SubjectScoreRepository.findByIdExpanded(
            tenantSchema = tenantSchema,
            id = scoreId
        ) ?: throw IllegalArgumentException("Subject score not found")

        val recordId = existing.academicRecord.id

        val ok = SubjectScoreRepository.deleteById(
            tenantSchema = tenantSchema,
            scoreId = scoreId
        )
        if (!ok) throw IllegalArgumentException("Subject score not found")

        AcademicRecordRepoLite.recomputeRawTotal(recordId)
        RankingService.recomputeAll(recordId)
    }
}