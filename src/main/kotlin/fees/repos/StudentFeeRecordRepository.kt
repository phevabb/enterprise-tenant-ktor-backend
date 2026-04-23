package com.example.fees.repos




import com.example.fees.mappers.toStudentFeeRecordModel
import com.example.fees.models.StudentFeeRecordModel
import com.example.fees.tables.FeeStructureTable
import com.example.fees.tables.StudentFeeRecordTable
import com.example.student.tables.AcademicYearTable
import com.example.student.StudentsTable
import com.example.student.tables.TermTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction


object StudentFeeRecordRepository {

    fun findAll(): List<StudentFeeRecordModel> = transaction {
        StudentFeeRecordTable
            .selectAll()
            .orderBy(StudentFeeRecordTable.id, SortOrder.DESC)
            .map { it.toStudentFeeRecordModel() }
    }

    fun findById(id: Int): StudentFeeRecordModel? = transaction {
        StudentFeeRecordTable
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
                otherTable = FeeStructureTable,
                joinType = JoinType.INNER,
                onColumn = StudentFeeRecordTable.feeStructure,
                otherColumn = FeeStructureTable.id
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