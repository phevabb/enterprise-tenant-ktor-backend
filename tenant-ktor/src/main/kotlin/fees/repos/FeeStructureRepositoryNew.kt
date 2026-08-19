package fees.repos


import com.example.academics.repos.setTenantSchema

import com.example.fees.tables.FeeStructureTable
import com.example.fees.tables.StudentFeeRecordTable
import com.example.student.StudentsTable
import com.example.student.tables.AcademicYearTable
import com.example.student.tables.NewGradeClassTable
import com.example.student.tables.TermTable
import fees.dtos.requests.CreateFeeStructureRequest
import fees.dtos.responses.FeeStructureCreateResponse
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object FeeStructureRepository {

    fun createFeeStructureAndGenerateStudentRecords(
        tenantSchema: String,
        request: CreateFeeStructureRequest
    ): FeeStructureCreateResponse {

        require(tenantSchema.isNotBlank()) {
            "Tenant schema is required."
        }

        require(request.academicYearId > 0) {
            "Academic year is required."
        }

        require(request.gradeClassId > 0) {
            "Class is required."
        }

        require(request.termId > 0) {
            "Term is required."
        }

        require(request.amount > 0) {
            "Fee amount must be greater than zero."
        }

        return transaction {

            setTenantSchema(
                tenantSchema
            )

            val academicYearEntityId =
                EntityID(
                    request.academicYearId,
                    AcademicYearTable
                )

            val gradeClassEntityId =
                EntityID(
                    request.gradeClassId,
                    NewGradeClassTable
                )

            val termEntityId =
                EntityID(
                    request.termId,
                    TermTable
                )

            val feeStructureId =
                FeeStructureTable.insert {

                    it[academic_year] =
                        academicYearEntityId

                    it[grade_class] =
                        gradeClassEntityId

                    it[term] =
                        termEntityId

                    it[amount] =
                        request.amount

                    it[is_discounted] =
                        request.isDiscounted

                } get FeeStructureTable.id

            val generated =
                generateStudentFeeRecordsForFeeStructureInternal(
                    feeStructureId = feeStructureId.value,
                    gradeClassId = request.gradeClassId,
                    feeAmount = request.amount
                )

            FeeStructureCreateResponse(
                success = true,
                message = "Fee structure created and student fee records generated successfully.",
                feeStructureId = feeStructureId.value,
                studentsFound = generated.studentsFound,
                recordsCreated = generated.recordsCreated,
                recordsSkipped = generated.recordsSkipped
            )
        }
    }

    fun generateStudentFeeRecordsForExistingFeeStructure(
        tenantSchema: String,
        feeStructureId: Int
    ): FeeStructureCreateResponse {

        require(tenantSchema.isNotBlank()) {
            "Tenant schema is required."
        }

        require(feeStructureId > 0) {
            "Fee structure id is required."
        }

        return transaction {

            setTenantSchema(
                tenantSchema
            )

            val feeStructureRow =
                FeeStructureTable
                    .selectAll()
                    .where {
                        FeeStructureTable.id eq feeStructureId
                    }
                    .singleOrNull()
                    ?: throw IllegalArgumentException(
                        "Fee structure not found."
                    )

            val gradeClassId =
                feeStructureRow[FeeStructureTable.grade_class].value

            val feeAmount =
                feeStructureRow[FeeStructureTable.amount]

            val generated =
                generateStudentFeeRecordsForFeeStructureInternal(
                    feeStructureId = feeStructureId,
                    gradeClassId = gradeClassId,
                    feeAmount = feeAmount
                )

            FeeStructureCreateResponse(
                success = true,
                message = "Student fee records generated successfully.",
                feeStructureId = feeStructureId,
                studentsFound = generated.studentsFound,
                recordsCreated = generated.recordsCreated,
                recordsSkipped = generated.recordsSkipped
            )
        }
    }

    private fun generateStudentFeeRecordsForFeeStructureInternal(
        feeStructureId: Int,
        gradeClassId: Int,
        feeAmount: Int
    ): GeneratedFeeRecordResult {

        val gradeClassEntityId =
            EntityID(
                gradeClassId,
                NewGradeClassTable
            )

        val feeStructureEntityId =
            EntityID(
                feeStructureId,
                FeeStructureTable
            )

        val studentsInClass =
            StudentsTable
                .selectAll()
                .where {
                    (StudentsTable.currentNewGradeClass eq gradeClassEntityId) and
                            (StudentsTable.isGraduated eq false)
                }
                .map { row ->
                    row[StudentsTable.id]
                }

        if (studentsInClass.isEmpty()) {

            return GeneratedFeeRecordResult(
                studentsFound = 0,
                recordsCreated = 0,
                recordsSkipped = 0
            )
        }

        val existingStudentIds =
            StudentFeeRecordTable
                .selectAll()
                .where {
                    StudentFeeRecordTable.feeStructure eq feeStructureEntityId
                }
                .map { row ->
                    row[StudentFeeRecordTable.student].value
                }
                .toSet()

        var createdCount =
            0

        var skippedCount =
            0

        val now =
            System.currentTimeMillis()

        studentsInClass.forEach { studentEntityId ->

            val studentId =
                studentEntityId.value

            if (existingStudentIds.contains(studentId)) {

                skippedCount += 1

            } else {

                StudentFeeRecordTable.insert {

                    it[student] =
                        studentEntityId

                    it[feeStructure] =
                        feeStructureEntityId

                    it[amountPaid] =
                        0

                    it[balance] =
                        feeAmount

                    it[isFullyPaid] =
                        false

                    it[dateCreated] =
                        now
                }

                createdCount += 1
            }
        }

        return GeneratedFeeRecordResult(
            studentsFound = studentsInClass.size,
            recordsCreated = createdCount,
            recordsSkipped = skippedCount
        )
    }

    private data class GeneratedFeeRecordResult(
        val studentsFound: Int,
        val recordsCreated: Int,
        val recordsSkipped: Int
    )
}