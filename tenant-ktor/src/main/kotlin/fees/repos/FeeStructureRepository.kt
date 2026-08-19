package com.example.fees.repos

import com.example.fees.dtos.responses.FeeStructureResponseDto
import com.example.fees.dtos.responses.toFeeStructureResponseDto
import com.example.fees.mappers.toFeeStructureModel

import com.example.fees.models.FeeStructureModel
import com.example.fees.tables.FeeStructureTable
import com.example.fees.tables.StudentFeeRecordTable
import com.example.student.StudentsTable
import com.example.student.tables.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction


object FeeStructureRepository {

    private fun Transaction.setTenantSchema(tenantSchema: String) {
        val safeSchema = tenantSchema.replace("\"", "\"\"")
        exec("""SET LOCAL search_path TO "$safeSchema"""")
    }



    data class GeneratedStudentFeeRecordsSummary(
        val feeStructureId: Int,
        val gradeClassId: Int,
        val studentsFound: Int,
        val recordsCreated: Int,
        val recordsSkipped: Int
    )

    fun generateStudentFeeRecordsForFeeStructure(
        tenantSchema: String,
        feeStructureId: Int
    ): GeneratedStudentFeeRecordsSummary {

        require(
            tenantSchema.isNotBlank()
        ) {
            "Tenant schema is required."
        }

        require(
            feeStructureId > 0
        ) {
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

            GeneratedStudentFeeRecordsSummary(
                feeStructureId = feeStructureId,
                gradeClassId = gradeClassId,
                studentsFound = studentsInClass.size,
                recordsCreated = createdCount,
                recordsSkipped = skippedCount
            )
        }
    }

    // ✅ CREATE
    fun create(
        tenantSchema: String,
        academicYearId: Int,
        gradeClassId: Int,
        termId: Int,
        amount: Int,
        isDiscounted: Boolean
    ) = transaction {

        setTenantSchema(tenantSchema)

        val id = FeeStructureTable.insertAndGetId {
            it[academic_year] = EntityID(academicYearId, AcademicYearTable)
            it[grade_class] = EntityID(gradeClassId, NewGradeClassTable)
            it[term] = EntityID(termId, TermTable)
            it[FeeStructureTable.amount] = amount
            it[is_discounted] = isDiscounted
        }.value

        findByIdWithRelations(
            tenantSchema = tenantSchema,
            id = id
        ) ?: error("FeeStructure created but not found")
    }

    // ✅ FIND BY ID (with joins)
    fun findByIdWithRelations(
        tenantSchema: String,
        id: Int
    ): FeeStructureResponseDto? = transaction {

        setTenantSchema(tenantSchema)

        FeeStructureTable
            .join(
                AcademicYearTable,
                JoinType.INNER,
                FeeStructureTable.academic_year,
                AcademicYearTable.id
            )
            .join(
                NewGradeClassTable,
                JoinType.INNER,
                FeeStructureTable.grade_class,
                NewGradeClassTable.id
            )
            .join(
                TermTable,
                JoinType.INNER,
                FeeStructureTable.term,
                TermTable.id
            )
            .selectAll()
            .where { FeeStructureTable.id eq id }
            .singleOrNull()
            ?.toFeeStructureResponseDto()
    }

    // ✅ LIST ALL
    fun findAll(
        tenantSchema: String
    ): List<FeeStructureResponseDto> = transaction {

        setTenantSchema(tenantSchema)

        FeeStructureTable
            .join(
                AcademicYearTable,
                JoinType.INNER,
                FeeStructureTable.academic_year,
                AcademicYearTable.id
            )
            .join(
                NewGradeClassTable,
                JoinType.INNER,
                FeeStructureTable.grade_class,
                NewGradeClassTable.id
            )
            .join(
                TermTable,
                JoinType.INNER,
                FeeStructureTable.term,
                TermTable.id
            )
            .selectAll()
            .orderBy(FeeStructureTable.id, SortOrder.DESC)
            .map { it.toFeeStructureResponseDto() }
    }


    fun findAllPaginated(
        tenantSchema: String,
        page: Int,
        limit: Int,
        search: String?
    ): Pair<List<FeeStructureResponseDto>, Long> = transaction {

        setTenantSchema(tenantSchema)

        val offset = ((page - 1) * limit).toLong()

        val query = FeeStructureTable
            .join(
                AcademicYearTable,
                JoinType.INNER,
                FeeStructureTable.academic_year,
                AcademicYearTable.id
            )
            .join(
                NewGradeClassTable,
                JoinType.INNER,
                FeeStructureTable.grade_class,
                NewGradeClassTable.id
            )
            .join(
                TermTable,
                JoinType.INNER,
                FeeStructureTable.term,
                TermTable.id
            )
            .selectAll()

        val filteredQuery = if (!search.isNullOrBlank()) {
            query.andWhere {
                (AcademicYearTable.name like "%$search%") or
                        (NewGradeClassTable.name like "%$search%") or
                        (TermTable.name like "%$search%")
            }
        } else {
            query
        }

        val total = filteredQuery.count()

        val result = filteredQuery
            .orderBy(FeeStructureTable.id, SortOrder.DESC)
            .limit(limit)
            .offset(offset)
            .map { it.toFeeStructureResponseDto() }

        Pair(result, total)
    }


    // ✅ PATCH (partial update)
    fun patch(
        tenantSchema: String,
        id: Int,
        amount: Int?,
        isDiscounted: Boolean?
    ): FeeStructureModel? = transaction {

        setTenantSchema(tenantSchema)

        val updated = FeeStructureTable.update(
            where = { FeeStructureTable.id eq id }
        ) {
            amount?.let { amt ->
                it[FeeStructureTable.amount] = amt
            }

            isDiscounted?.let { d ->
                it[is_discounted] = d
            }
        }

        if (updated == 0) {
            null
        } else {
            FeeStructureTable
                .selectAll()
                .where { FeeStructureTable.id eq id }
                .single()
                .toFeeStructureModel()
        }
    }

    // ✅ DELETE
    fun delete(
        tenantSchema: String,
        id: Int
    ): Boolean = transaction {

        setTenantSchema(tenantSchema)

        FeeStructureTable.deleteWhere {
            FeeStructureTable.id eq id
        } > 0
    }
}