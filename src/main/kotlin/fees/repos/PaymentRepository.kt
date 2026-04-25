package com.example.fees.repos

import com.example.account.AccountTable
import com.example.fees.dtos.responses.PaymentResponseDto
import com.example.fees.dtos.responses.toPaymentResponseDto
import com.example.fees.tables.FeeStructureTable
import com.example.fees.tables.PaymentTable
import com.example.fees.tables.StudentFeeRecordTable
import com.example.student.StudentsTable
import com.example.student.tables.AcademicYearTable
import com.example.student.tables.NewGradeClassTable
import com.example.student.tables.TermTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.DeleteStatement.Companion.where
import org.jetbrains.exposed.sql.transactions.transaction

object PaymentRepository {
    fun create(
        student_fee_record: Int,
        amount: Int,
    ) = transaction {
        val id = PaymentTable.insertAndGetId {
            it[PaymentTable.student_fee_record] = EntityID(student_fee_record, StudentFeeRecordTable)
            it[PaymentTable.amount] = amount
            it[date_created] = System.currentTimeMillis()

        }.value
        findById(id)?: error("$id not found")
    }



    fun findById(id: Int): PaymentResponseDto? = transaction {
        PaymentTable
            .join(
                otherTable = StudentFeeRecordTable,
                joinType = JoinType.INNER,
                onColumn = PaymentTable.student_fee_record,
                otherColumn = StudentFeeRecordTable.id
            )
            .join(
                otherTable = StudentsTable,
                joinType = JoinType.INNER,
                onColumn = StudentFeeRecordTable.student,      // <-- your FK column
                otherColumn = StudentsTable.id
            )
            .join(
                otherTable = AccountTable,
                joinType = JoinType.INNER,
                onColumn = StudentsTable.user,                 // <-- your FK column
                otherColumn = AccountTable.id
            )
            .join(
                otherTable = FeeStructureTable,
                joinType = JoinType.INNER,
                onColumn = StudentFeeRecordTable.feeStructure, // <-- your FK column
                otherColumn = FeeStructureTable.id
            )
            // ✅ Explicit join removes ambiguity:
            .join(
                otherTable = NewGradeClassTable,
                joinType = JoinType.INNER,
                onColumn = FeeStructureTable.grade_class,      // <-- IMPORTANT: use fee structure FK
                otherColumn = NewGradeClassTable.id
            )
            .join(
                otherTable = AcademicYearTable,
                joinType = JoinType.INNER,
                onColumn = FeeStructureTable.academic_year,
                otherColumn = AcademicYearTable.id
            )
            .join(
                otherTable = TermTable,
                joinType = JoinType.INNER,
                onColumn = FeeStructureTable.term,
                otherColumn = TermTable.id
            )
            .selectAll()
            .where{ PaymentTable.id eq id }
            .singleOrNull()
            ?.toPaymentResponseDto()
    }

    fun findAll(): List<PaymentResponseDto> = transaction {

        PaymentTable
            .join(
                otherTable = StudentFeeRecordTable,
                joinType = JoinType.INNER,
                onColumn = PaymentTable.student_fee_record,
                otherColumn = StudentFeeRecordTable.id
            )
            .join(
                otherTable = StudentsTable,
                joinType = JoinType.INNER,
                onColumn = StudentFeeRecordTable.student,      // <-- your FK column
                otherColumn = StudentsTable.id
            )
            .join(
                otherTable = AccountTable,
                joinType = JoinType.INNER,
                onColumn = StudentsTable.user,                 // <-- your FK column
                otherColumn = AccountTable.id
            )
            .join(
                otherTable = FeeStructureTable,
                joinType = JoinType.INNER,
                onColumn = StudentFeeRecordTable.feeStructure, // <-- your FK column
                otherColumn = FeeStructureTable.id
            )
            // ✅ Explicit join removes ambiguity:
            .join(
                otherTable = NewGradeClassTable,
                joinType = JoinType.INNER,
                onColumn = FeeStructureTable.grade_class,      // <-- IMPORTANT: use fee structure FK
                otherColumn = NewGradeClassTable.id
            )
            .join(
                otherTable = AcademicYearTable,
                joinType = JoinType.INNER,
                onColumn = FeeStructureTable.academic_year,
                otherColumn = AcademicYearTable.id
            )
            .join(
                otherTable = TermTable,
                joinType = JoinType.INNER,
                onColumn = FeeStructureTable.term,
                otherColumn = TermTable.id
            )
            .selectAll()
            .orderBy(PaymentTable.id, SortOrder.DESC)
            .map {it.toPaymentResponseDto()}
    }



}