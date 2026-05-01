package com.example.familyfees.repos

import com.example.account.AccountTable
import com.example.familyfees.tables.FamilyTable
import com.example.familyfees.dtos.responses.FamilyPaymentResponseDto
import com.example.familyfees.dtos.responses.toFamilyPaymentResponseDto
import com.example.familyfees.tables.FamilyFeeRecordTable
import com.example.familyfees.tables.FamilyPaymentTable
import com.example.fees.dtos.responses.PaymentResponseDto
import com.example.fees.dtos.responses.toPaymentResponseDto
import com.example.fees.tables.FeeStructureTable
import com.example.fees.tables.PaymentTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import com.example.fees.tables.PaymentTable.date_created
import com.example.fees.tables.StudentFeeRecordTable
import com.example.student.StudentsTable
import com.example.student.tables.AcademicYearTable
import com.example.student.tables.NewGradeClassTable
import com.example.student.tables.TermTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

object FamilyPaymentRepository {
    fun create(
        family_fee_record: Int,
        amount: Int,
    ) = transaction {

        val id = FamilyPaymentTable.insertAndGetId {
            it[FamilyPaymentTable.family_fee_record] = EntityID(family_fee_record, FamilyPaymentTable)
            it[FamilyPaymentTable.amount] = amount
            it[date_created] = System.currentTimeMillis()

        }.value


    }

    fun findById(id: Int): FamilyPaymentResponseDto? = transaction {
        FamilyPaymentTable
            .join(
                otherTable = FamilyFeeRecordTable,
                joinType = JoinType.INNER,
                onColumn = FamilyPaymentTable.family_fee_record,
                otherColumn = FamilyFeeRecordTable.id
            )
            .join(
                otherTable = FamilyTable,
                joinType = JoinType.INNER,
                onColumn = FamilyFeeRecordTable.family,      // <-- your FK column
                otherColumn =  FamilyTable.id
            )




            .selectAll()
            .where{ FamilyPaymentTable.id eq id }
            .singleOrNull()
            ?.toFamilyPaymentResponseDto()
    }

    fun findAll(): List<FamilyPaymentResponseDto> = transaction {

        FamilyPaymentTable
            .join(
                FamilyFeeRecordTable,
                JoinType.INNER,
                onColumn = FamilyPaymentTable.family_fee_record,
                otherColumn = FamilyFeeRecordTable.id
            )
            .join(
                FamilyTable,
                JoinType.LEFT,
                onColumn = FamilyFeeRecordTable.family,
                otherColumn = FamilyTable.id
            )
            .join(
                TermTable,
                JoinType.LEFT,
                onColumn = FamilyFeeRecordTable.term,
                otherColumn = TermTable.id
            )
            .join(
                AcademicYearTable,
                JoinType.LEFT,
                onColumn = FamilyFeeRecordTable.academic_year,
                otherColumn = AcademicYearTable.id
            )
            .selectAll()
            .orderBy(FamilyPaymentTable.id, SortOrder.DESC)
            .map { row ->
                row.toFamilyPaymentResponseDto()
            }
    }

    fun createPaymentAndUpdateFfr(
        familyFeeRecordId: Int,
        amount: Int,
        paymentMethod: String
    ): FamilyPaymentResponseDto = transaction {

        // 1️⃣ Fetch the family fee record
        val feeRecord = FamilyFeeRecordTable
            .selectAll()
            .where { FamilyFeeRecordTable.id eq familyFeeRecordId }
            .singleOrNull()
            ?: throw IllegalArgumentException("Family fee record not found")

        val currentPaid = feeRecord[FamilyFeeRecordTable.amount_paid]
        val amountToPay = feeRecord[FamilyFeeRecordTable.amount_to_pay]

        val newPaidAmount = currentPaid + amount
        val newBalance = amountToPay - newPaidAmount
        val isFullyPaid = newBalance <= 0

        // 2️⃣ Insert payment
        val paymentId = FamilyPaymentTable.insertAndGetId {
            it[family_fee_record] = familyFeeRecordId
            it[FamilyPaymentTable.amount] = amount
            it[FamilyPaymentTable.payment_method] = paymentMethod
            it[FamilyPaymentTable.date_created] = System.currentTimeMillis()
        }

        // 3️⃣ Update family fee record
        FamilyFeeRecordTable.update(
            { FamilyFeeRecordTable.id eq familyFeeRecordId }
        ) {
            it[amount_paid] = newPaidAmount
            it[balance] = newBalance
            it[is_fully_paid] = isFullyPaid
        }

        // 4️⃣ Re‑query everything for response
        FamilyPaymentTable
            .join(
                FamilyFeeRecordTable,
                JoinType.INNER,
                onColumn = FamilyPaymentTable.family_fee_record,
                otherColumn = FamilyFeeRecordTable.id
            )
            .join(
                FamilyTable,
                JoinType.LEFT,
                onColumn = FamilyFeeRecordTable.family,
                otherColumn = FamilyTable.id
            )
            .join(
                TermTable,
                JoinType.LEFT,
                onColumn = FamilyFeeRecordTable.term,
                otherColumn = TermTable.id
            )
            .join(
                AcademicYearTable,
                JoinType.LEFT,
                onColumn = FamilyFeeRecordTable.academic_year,
                otherColumn = AcademicYearTable.id
            )
            .selectAll()
            .where { FamilyPaymentTable.id eq paymentId }
            .single()
            .toFamilyPaymentResponseDto()
    }

    fun delete(id: Int): Boolean = transaction {
        FamilyPaymentTable.deleteWhere { FamilyPaymentTable.id eq id } > 0
    }
}