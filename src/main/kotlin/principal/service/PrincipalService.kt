package com.example.principal.service

import com.example.account.AccountRepository

import com.example.account.AccountTable
import com.example.fees.tables.FeeStructureTable
import com.example.fees.tables.PaymentTable
import com.example.fees.tables.StudentFeeRecordTable
import com.example.principal.dtos.requests.CreatePrincipalRequest
import com.example.principal.dtos.responses.ExpectedFeesResponse
import com.example.principal.repos.PrincipalRepository
import com.example.student.tables.AcademicYearTable
import com.example.student.tables.NewGradeClassTable
import com.example.student.tables.TermTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.innerJoin

import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.sum
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.random.Random

object PrincipalService {
    private fun hashPassword(raw: String): String {
        return raw.reversed() // placeholder
    }

    fun createPrincipal(req: CreatePrincipalRequest) = transaction {

        val pin = Random.nextInt(1000, 10000).toString()

        val userId = AccountTable.insertAndGetId {
            it[userId] = generateUniqueUserId()
            it[fullName] = req.user.fullName
            it[gender] = req.user.gender
            it[dateOfBirth] = req.user.dateOfBirth
            it[nationality] = req.user.nationality

            // ✅ FORCE role
            it[role] = "principal"

            it[isActive] = req.user.isActive
            it[isStaff] = true

            // ✅ PIN + HASH
            it[AccountTable.pin] = pin
            it[passwordHash] = hashPassword(pin)
        }.value

        PrincipalRepository.create(userId)
    }

    fun deletePrincipal(id: Int): Boolean {
        return PrincipalRepository.delete(id)
    }

    private fun generateUniqueUserId(): String {
        while (true) {
            val candidate = Random.nextInt(10_000_000, 99_999_999).toString()
            if (!AccountRepository.existsByUserId(candidate)) {
                return candidate
            }
        }
    }

    fun expectedFeesSummary(): List<ExpectedFeesResponse> = transaction {

        val expectedExpr = (StudentFeeRecordTable.amountPaid + StudentFeeRecordTable.balance).sum()
        val collectedExpr = StudentFeeRecordTable.amountPaid.sum()
        val pendingExpr = StudentFeeRecordTable.balance.sum()

        PaymentTable
            .innerJoin(
                StudentFeeRecordTable,
                { PaymentTable.student_fee_record },
                { StudentFeeRecordTable.id }
            )
            .innerJoin(
                FeeStructureTable,
                { StudentFeeRecordTable.feeStructure },
                { FeeStructureTable.id }
            )
            .innerJoin(
                NewGradeClassTable,
                { FeeStructureTable.grade_class },
                { NewGradeClassTable.id }
            )
            .innerJoin(
                TermTable,
                { FeeStructureTable.term },
                { TermTable.id }
            )
            // ✅ ✅ EXPLICIT join (THIS FIXES YOUR ERROR)
            .innerJoin(
                AcademicYearTable,
                { FeeStructureTable.academic_year },
                { AcademicYearTable.id }
            )
            .select(
                NewGradeClassTable.name,
                TermTable.name,
                AcademicYearTable.name,
                expectedExpr,
                collectedExpr,
                pendingExpr
            )
            .groupBy(
                NewGradeClassTable.name,
                TermTable.name,
                AcademicYearTable.name
            )
            .orderBy(
                AcademicYearTable.name to SortOrder.ASC,
                TermTable.name to SortOrder.ASC,
                NewGradeClassTable.name to SortOrder.ASC
            )
            .map { row ->

                ExpectedFeesResponse(
                    className = row[NewGradeClassTable.name],
                    term = row[TermTable.name],
                    academicYear = row[AcademicYearTable.name],

                    expectedAmount = (row[expectedExpr] ?: 0).toDouble(),
                    collectedAmount = (row[collectedExpr] ?: 0).toDouble(),
                    pendingAmount = (row[pendingExpr] ?: 0).toDouble()
                )
            }
    }
}