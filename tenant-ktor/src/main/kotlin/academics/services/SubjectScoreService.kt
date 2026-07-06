package com.example.academics.services

import com.example.academics.dtos.requests.CreateBulkSubjectScoreBySubjectRequest
import com.example.academics.dtos.requests.CreateOrUpdateSubjectScoreRequest

import com.example.academics.dtos.response.SubjectScoreExpandedResponse
import com.example.academics.repos.AcademicRecordRepoLite
import com.example.academics.repos.SubjectScoreRepository
import org.jetbrains.exposed.sql.transactions.transaction






import com.example.academics.dtos.requests.CreateSubjectScoreByStudentRequest
import com.example.academics.dtos.requests.CreateSubjectScoresByStudentRequest

import com.example.academics.repos.SubjectRepoLite

import com.example.student.repos.AcademicYearRepository
import com.example.student.repos.StudentLiteRepo
import com.example.student.repos.TermRepository

import com.example.academics.dtos.requests.PatchSubjectScoreRequest
import com.example.academics.repos.setTenantSchema


object SubjectScoreService {


    fun createOrUpdateManyStudentsBySubject(
        tenantSchema: String,
        req: CreateBulkSubjectScoreBySubjectRequest
    ): List<SubjectScoreExpandedResponse> = transaction {
        setTenantSchema(tenantSchema)

        val start = System.currentTimeMillis()

        if (req.subject.isBlank()) {
            throw IllegalArgumentException("Subject is required.")
        }

        if (req.scores.isEmpty()) {
            throw IllegalArgumentException("No scores provided.")
        }

        val subjectId = SubjectRepoLite.findIdByIdOrName(
            tenantSchema = tenantSchema,
            value = req.subject
        ) ?: throw IllegalArgumentException("Invalid subject: ${req.subject}")

        val (termId, yearId) = TermRepository.getCurrent(tenantSchema)
            ?: throw IllegalArgumentException("No current term found in database.")

        val savedIds = mutableListOf<Int>()
        val affectedRecordIds = mutableSetOf<Int>()

        req.scores.forEach { item ->
            val classLevelId = StudentLiteRepo.getStudentClassLevelId(
                tenantSchema = tenantSchema,
                studentId = item.student
            ) ?: throw IllegalArgumentException(
                "Student ${item.student} has no assigned class."
            )

            val recordId = AcademicRecordRepoLite.getOrCreate(
                studentId = item.student,
                termId = termId,
                yearId = yearId,
                classLevelId = classLevelId
            )

            val saved = SubjectScoreRepository.createOrUpdate(
                tenantSchema = tenantSchema,
                req = CreateOrUpdateSubjectScoreRequest(
                    academicRecordId = recordId,
                    subjectId = subjectId,
                    classScore = item.classScore,
                    examScore = item.examScore
                )
            )

            savedIds += saved.id
            affectedRecordIds += recordId
        }

        /**
         * Recompute each student's raw total once.
         */
        affectedRecordIds.forEach { recordId ->
            AcademicRecordRepoLite.recomputeRawTotal(recordId)
        }

        /**
         * Recompute ranking once, not per student.
         * Any affected record belongs to the same current class/term context in this teacher screen.
         */
        affectedRecordIds.firstOrNull()?.let { firstRecordId ->
            RankingService.recomputeAll(firstRecordId)
        }

        val responses = savedIds.mapNotNull { scoreId ->
            SubjectScoreRepository.findByIdExpanded(
                tenantSchema = tenantSchema,
                id = scoreId
            )
        }

        println(
            "createOrUpdateManyStudentsBySubject saved ${req.scores.size} scores in ${System.currentTimeMillis() - start}ms"
        )

        responses
    }

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

        val subjectId = SubjectRepoLite.findIdByIdOrName(
            tenantSchema = tenantSchema,
            value = req.subject
        ) ?: throw IllegalArgumentException("Invalid subject")

        val (termId, yearId) = TermRepository.getCurrent(tenantSchema)
            ?: throw IllegalArgumentException("No term found in database")

        val classLevelId = StudentLiteRepo.getStudentClassLevelId(
            tenantSchema = tenantSchema,
            studentId = studentId
        ) ?: throw IllegalArgumentException("Student has no assigned class")

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

        /**
         * Avoid this on every single subject save if the frontend uses bulk save.
         * RankingService.recomputeAll(recordId)
         */

        SubjectScoreRepository.findByIdExpanded(
            tenantSchema = tenantSchema,
            id = saved.id
        ) ?: throw IllegalStateException("Score saved but not found")
    }


    fun createOrUpdateManyByStudent(
        tenantSchema: String,
        req: CreateSubjectScoresByStudentRequest
    ): List<SubjectScoreExpandedResponse> = transaction {
        setTenantSchema(tenantSchema)

        val start = System.currentTimeMillis()

        val studentId = req.student

        if (req.scores.isEmpty()) {
            throw IllegalArgumentException("No scores provided.")
        }

        val (termId, yearId) = TermRepository.getCurrent(tenantSchema)
            ?: throw IllegalArgumentException("No term found in database")

        val classLevelId = StudentLiteRepo.getStudentClassLevelId(
            tenantSchema = tenantSchema,
            studentId = studentId
        ) ?: throw IllegalArgumentException("Student has no assigned class")

        val recordId = AcademicRecordRepoLite.getOrCreate(
            studentId = studentId,
            termId = termId,
            yearId = yearId,
            classLevelId = classLevelId
        )

        val savedIds = mutableListOf<Int>()

        req.scores.forEach { score ->
            val subjectId = SubjectRepoLite.findIdByIdOrName(
                tenantSchema = tenantSchema,
                value = score.subject
            ) ?: throw IllegalArgumentException("Invalid subject: ${score.subject}")

            val saved = SubjectScoreRepository.createOrUpdate(
                tenantSchema = tenantSchema,
                req = CreateOrUpdateSubjectScoreRequest(
                    academicRecordId = recordId,
                    subjectId = subjectId,
                    classScore = score.classScore,
                    examScore = score.examScore
                )
            )

            savedIds += saved.id
        }

        /**
         * Important:
         * Recompute total only once.
         */
        AcademicRecordRepoLite.recomputeRawTotal(recordId)

        /**
         * * Important:
         * Recompute ran*ing only once after all scores are saved.
         */
        RankingService.recomputeAll(recordId)

        val responses = savedIds.mapNotNull { scoreId ->
            SubjectScoreRepository.findByIdExpanded(
                tenantSchema = tenantSchema,
                id = scoreId
            )
        }

        println(
            "createOrUpdateManyByStudent saved ${req.scores.size} scores in ${System.currentTimeMillis() - start}ms"
        )

        responses
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