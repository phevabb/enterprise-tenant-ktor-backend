package com.example.fees.repos


import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.transactions.transaction
import com.example.account.AccountTable
import com.example.fees.mappers.toStudentFeeRecordModel
import com.example.fees.models.StudentFeeRecordModel
import com.example.fees.tables.FeeStructureTable
import com.example.fees.tables.StudentFeeRecordTable
import com.example.student.tables.AcademicYearTable
import com.example.student.StudentsTable
import com.example.student.tables.NewGradeClassTable
import com.example.student.tables.TermTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.transactions.transaction



object StudentFeeRecordRepository {

    fun findAll(): List<StudentFeeRecordModel> = transaction {

        StudentFeeRecordTable
            .join(
                StudentsTable,
                JoinType.INNER,
                StudentFeeRecordTable.student,
                StudentsTable.id
            )
            .join(
                AccountTable,
                JoinType.INNER,
                StudentsTable.user,
                AccountTable.id
            )
            .join(
                FeeStructureTable,
                JoinType.INNER,
                StudentFeeRecordTable.feeStructure,
                FeeStructureTable.id
            )
            .join(
                AcademicYearTable,
                JoinType.INNER,
                FeeStructureTable.academic_year,
                AcademicYearTable.id
            )
            .join(
                TermTable,
                JoinType.INNER,
                FeeStructureTable.term,
                TermTable.id
            )
            .join(
                NewGradeClassTable,
                JoinType.INNER,
                FeeStructureTable.grade_class,
                NewGradeClassTable.id
            )
            .selectAll()
            .orderBy(StudentFeeRecordTable.id, SortOrder.DESC)
            .map { it.toStudentFeeRecordModel() }
    }








        fun findAllPaginated(
            page: Int,
            limit: Int,
            search: String?,
            feeStructureId: Int?,
            isFullyPaid: Boolean?
        ): Pair<List<StudentFeeRecordModel>, Long> = transaction {

            val safePage = if (page < 1) 1 else page
            val safeLimit = if (limit < 1) 20 else limit
            val offset = ((safePage - 1) * safeLimit).toLong()

            val baseQuery = StudentFeeRecordTable
                .join(
                    StudentsTable,
                    JoinType.INNER,
                    StudentFeeRecordTable.student,
                    StudentsTable.id
                )
                .join(
                    AccountTable,
                    JoinType.INNER,
                    StudentsTable.user,
                    AccountTable.id
                )
                .join(
                    FeeStructureTable,
                    JoinType.INNER,
                    StudentFeeRecordTable.feeStructure,
                    FeeStructureTable.id
                )
                .join(
                    AcademicYearTable,
                    JoinType.INNER,
                    FeeStructureTable.academic_year,
                    AcademicYearTable.id
                )
                .join(
                    TermTable,
                    JoinType.INNER,
                    FeeStructureTable.term,
                    TermTable.id
                )
                .join(
                    NewGradeClassTable,
                    JoinType.INNER,
                    FeeStructureTable.grade_class,
                    NewGradeClassTable.id
                )
                .selectAll()

            // -----------------------------
            // ✅ Apply fee_structure_id filter
            // -----------------------------
            val withFeeStructureFilter = if (feeStructureId != null) {
                baseQuery.andWhere { FeeStructureTable.id eq feeStructureId }
            } else baseQuery

            // -----------------------------
            // ✅ Apply is_fully_paid filter (balance == 0)
            // -----------------------------
            val withPaidFilter = if (isFullyPaid != null) {
                if (isFullyPaid) {
                    withFeeStructureFilter.andWhere { StudentFeeRecordTable.balance eq 0 }
                } else {
                    withFeeStructureFilter.andWhere { StudentFeeRecordTable.balance greater 0 }
                }
            } else withFeeStructureFilter

            // -----------------------------
            // ✅ Case-insensitive search (DB-agnostic)
            // -----------------------------
            val finalQuery = if (!search.isNullOrBlank()) {
                val pattern = "%${search.lowercase()}%"
                withPaidFilter.andWhere {
                    (AccountTable.fullName.lowerCase() like pattern) or
                            (NewGradeClassTable.name.lowerCase() like pattern) or
                            (TermTable.name.lowerCase() like pattern) or
                            (AcademicYearTable.name.lowerCase() like pattern)
                }
            } else withPaidFilter

            val total = finalQuery.count()

            val items = finalQuery
                .orderBy(StudentFeeRecordTable.id, SortOrder.DESC)
                .limit(safeLimit)
                .offset(offset)
                .map { it.toStudentFeeRecordModel() }

            Pair(items, total)
        }






    private fun baseStudentFeeRecordQuery() =
        StudentFeeRecordTable
            .join(
                StudentsTable,
                JoinType.INNER,
                StudentFeeRecordTable.student,
                StudentsTable.id
            )
            .join(
                AccountTable,
                JoinType.INNER,
                StudentsTable.user,
                AccountTable.id
            )
            .join(
                FeeStructureTable,
                JoinType.INNER,
                StudentFeeRecordTable.feeStructure,
                FeeStructureTable.id
            )
            .join(
                AcademicYearTable,
                JoinType.INNER,
                FeeStructureTable.academic_year,
                AcademicYearTable.id
            )
            .join(
                TermTable,
                JoinType.INNER,
                FeeStructureTable.term,
                TermTable.id
            )
            .join(
                NewGradeClassTable,
                JoinType.INNER,
                FeeStructureTable.grade_class,
                NewGradeClassTable.id
            )
    fun findById(id: Int): StudentFeeRecordModel? = transaction {
        baseStudentFeeRecordQuery()
            .selectAll()
            .where { StudentFeeRecordTable.id eq id }
            .singleOrNull()
            ?.toStudentFeeRecordModel()
    }

    fun findByStudent(studentId: Int): List<StudentFeeRecordModel> = transaction {
        StudentFeeRecordTable
            .selectAll()
            .where { StudentFeeRecordTable.student eq EntityID(studentId, StudentsTable) }
            .orderBy(StudentFeeRecordTable.id, SortOrder.DESC)
            .map { it.toStudentFeeRecordModel() }
    }

    /**
     * Create a record (assignment + initial balance).
     * Enforces: one fee structure per student per term (by query check).
     */
    fun create(studentId: Int, feeStructureId: Int): StudentFeeRecordModel = transaction {

        // 1) Load fee structure (amount + term)
        val fsRow = FeeStructureTable
            .selectAll()
            .where { FeeStructureTable.id eq feeStructureId }
            .singleOrNull()
            ?: error("FeeStructure $feeStructureId not found")

        val termId = fsRow[FeeStructureTable.term].value
        val totalFee = fsRow[FeeStructureTable.amount]

        // 2) Enforce: student has no other fee record in THIS term
        val existsForTerm = StudentFeeRecordTable

            .join(
                StudentsTable,
                JoinType.INNER,
                StudentFeeRecordTable.student,
                StudentsTable.id
            )
            .join(
                AccountTable,
                JoinType.INNER,
                StudentsTable.user,
                AccountTable.id
            )
            .join(
                otherTable = FeeStructureTable,
                joinType = JoinType.INNER,
                onColumn = StudentFeeRecordTable.feeStructure,
                otherColumn = FeeStructureTable.id
            )
            .join(
                AcademicYearTable,
                JoinType.INNER,
                FeeStructureTable.academic_year,
                AcademicYearTable.id
            )
            .join(
                TermTable,
                JoinType.INNER,
                FeeStructureTable.term,
                TermTable.id
            )
            .join(
                NewGradeClassTable,
                JoinType.INNER,
                FeeStructureTable.grade_class,
                NewGradeClassTable.id
            )

            .selectAll()
            .where {
                (StudentFeeRecordTable.student eq EntityID(studentId, StudentsTable)) and
                        (FeeStructureTable.term eq EntityID(termId, TermTable))
            }
            .any()

        if (existsForTerm) {
            error("Student $studentId already has a fee structure for term $termId")
        }

        // 3) Insert StudentFeeRecord
        val newId = StudentFeeRecordTable.insertAndGetId {
            it[student] = EntityID(studentId, StudentsTable)
            it[feeStructure] = EntityID(feeStructureId, FeeStructureTable)

            it[amountPaid] = 0
            it[balance] = totalFee
            it[isFullyPaid] = (totalFee == 0)

            // no java.time: store epoch millis
            it[dateCreated] = System.currentTimeMillis()
        }.value

        findById(newId) ?: error("StudentFeeRecord created but not found")
    }

    /**
     * Add payment and recompute balance/isFullyPaid.
     */
    fun addPayment(recordId: Int, payment: Int): StudentFeeRecordModel? = transaction {
        if (payment <= 0) return@transaction null

        val recRow = StudentFeeRecordTable
            .selectAll()
            .where { StudentFeeRecordTable.id eq recordId }
            .singleOrNull()
            ?: return@transaction null

        val feeStructureId = recRow[StudentFeeRecordTable.feeStructure].value

        val fsRow = FeeStructureTable
            .selectAll()
            .where { FeeStructureTable.id eq feeStructureId }
            .singleOrNull()
            ?: return@transaction null

        val totalFee = fsRow[FeeStructureTable.amount]

        val currentPaid = recRow[StudentFeeRecordTable.amountPaid]
        val newPaid = currentPaid + payment
        val newBalance = (totalFee - newPaid).coerceAtLeast(0)
        val fullyPaid = (newBalance == 0)

        val updated = StudentFeeRecordTable.update({ StudentFeeRecordTable.id eq recordId }) { u ->
            u[amountPaid] = newPaid
            u[balance] = newBalance
            u[isFullyPaid] = fullyPaid
        }

        if (updated == 0) null else findById(recordId)
    }

    /**
     * Total arrears across same academic year for a student:
     * SUM(balance) where balance > 0 and fee_structure.academic_year = academicYearId
     */
    fun totalArrears(studentId: Int, academicYearId: Int, excludeRecordId: Int? = null): Int = transaction {

        val sumAlias = StudentFeeRecordTable.balance.sum().alias("total_balance")

        val base = StudentFeeRecordTable
            .join(
                otherTable = FeeStructureTable,
                joinType = JoinType.INNER,
                onColumn = StudentFeeRecordTable.feeStructure,
                otherColumn = FeeStructureTable.id
            )
            .select(sumAlias)
            .where {
                (StudentFeeRecordTable.student eq EntityID(studentId, StudentsTable)) and
                        (FeeStructureTable.academic_year eq EntityID(academicYearId, AcademicYearTable)) and
                        (StudentFeeRecordTable.balance greater 0)
            }

        val finalQuery = if (excludeRecordId != null) {
            base.andWhere { StudentFeeRecordTable.id neq excludeRecordId }
        } else base

        finalQuery.singleOrNull()?.get(sumAlias) ?: 0
    }

    fun delete(id: Int): Boolean = transaction {
        StudentFeeRecordTable.deleteWhere { StudentFeeRecordTable.id eq id } > 0
    }
}