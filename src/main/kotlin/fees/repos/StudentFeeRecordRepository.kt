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

    fun findAll(tenantSchema: String): List<StudentFeeRecordModel> = transaction {
        setTenantSchema(tenantSchema)

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
        tenantSchema: String,
        page: Int,
        limit: Int,
        search: String?,
        feeStructureId: Int?,
        isFullyPaid: Boolean?
    ): Pair<List<StudentFeeRecordModel>, Long> = transaction {

        setTenantSchema(tenantSchema)

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

        val withFeeStructureFilter = if (feeStructureId != null) {
            baseQuery.andWhere { FeeStructureTable.id eq feeStructureId }
        } else baseQuery

        val withPaidFilter = if (isFullyPaid != null) {
            if (isFullyPaid) {
                withFeeStructureFilter.andWhere {
                    StudentFeeRecordTable.balance eq 0
                }
            } else {
                withFeeStructureFilter.andWhere {
                    StudentFeeRecordTable.balance greater 0
                }
            }
        } else withFeeStructureFilter

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
    private fun Transaction.setTenantSchema(tenantSchema: String) {
        val safeSchema = tenantSchema.replace("\"", "\"\"")
        exec("""SET LOCAL search_path TO "$safeSchema"""")
    }

    fun findById(
        tenantSchema: String,
        id: Int
    ): StudentFeeRecordModel? = transaction {

        setTenantSchema(tenantSchema)

        baseStudentFeeRecordQuery()
            .selectAll()
            .where { StudentFeeRecordTable.id eq id }
            .singleOrNull()
            ?.toStudentFeeRecordModel()
    }

    fun findByStudent(
        tenantSchema: String,
        studentId: Int
    ): List<StudentFeeRecordModel> = transaction {

        setTenantSchema(tenantSchema)

        StudentFeeRecordTable
            .selectAll()
            .where {
                StudentFeeRecordTable.student eq EntityID(studentId, StudentsTable)
            }
            .orderBy(StudentFeeRecordTable.id, SortOrder.DESC)
            .map { it.toStudentFeeRecordModel() }
    }

    fun create(
        tenantSchema: String,
        studentId: Int,
        feeStructureId: Int
    ): StudentFeeRecordModel = transaction {

        setTenantSchema(tenantSchema)

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

        findById(tenantSchema,newId) ?: error("StudentFeeRecord created but not found")
    }


    fun addPayment(
        tenantSchema: String,
        recordId: Int,
        payment: Int
    ): StudentFeeRecordModel? = transaction {

        setTenantSchema(tenantSchema)

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

        if (updated == 0) {
            null
        } else {
            findById(tenantSchema, recordId)
        }
    }

    fun totalArrears(
        tenantSchema: String,
        studentId: Int,
        academicYearId: Int,
        excludeRecordId: Int? = null
    ): Int = transaction {

        setTenantSchema(tenantSchema)




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

    fun delete(
        tenantSchema: String,
        id: Int
    ): Boolean = transaction {

        setTenantSchema(tenantSchema)

        StudentFeeRecordTable.deleteWhere {
            StudentFeeRecordTable.id eq id
        } > 0
    }
}