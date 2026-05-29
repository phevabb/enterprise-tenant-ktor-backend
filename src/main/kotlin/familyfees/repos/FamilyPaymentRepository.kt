package com.example.familyfees.repos

import com.example.account.AccountTable
import com.example.familyfees.tables.FamilyTable
import com.example.familyfees.dtos.responses.FamilyPaymentResponseDto
import com.example.familyfees.dtos.responses.toFamilyPaymentResponseDto
import com.example.familyfees.mappers.toFamilyFeeRecordMinimalAfterUpdate
import com.example.familyfees.tables.FamilyFeeRecordTable
import com.example.familyfees.tables.FamilyPaymentTable
import com.example.fees.dtos.responses.PaymentResponseDto
import com.example.fees.dtos.responses.toPaymentResponseDto
import com.example.fees.tables.FeeStructureTable
import com.example.fees.tables.PaymentTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import com.example.fees.tables.PaymentTable.date_created
import com.example.fees.tables.StudentFeeRecordTable
import com.example.notifications.SmsPayload
import com.example.student.StudentsTable
import com.example.student.mappers.toStudentProfile
import com.example.student.tables.AcademicYearTable
import com.example.student.tables.NewGradeClassTable
import com.example.student.tables.TermTable
import io.ktor.server.plugins.BadRequestException
import kotlinx.serialization.Serializable
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
    ): FamilyPaymentResponseDto = transaction {

        // 1) Fetch the family fee record (so we can validate + get family id)
        val recordRow = FamilyFeeRecordTable
            .selectAll()
            .where { FamilyFeeRecordTable.id eq family_fee_record }
            .singleOrNull()
            ?: throw BadRequestException("Family fee record not found")

        val familyId = recordRow[FamilyFeeRecordTable.family]
        val currentBalance = recordRow[FamilyFeeRecordTable.balance]
        val amountToPay = recordRow[FamilyFeeRecordTable.amount_to_pay]
        val alreadyPaid = recordRow[FamilyFeeRecordTable.amount_paid]

        if (amount <= 0) throw BadRequestException("Payment amount must be greater than zero")
        if (amount > currentBalance) {
            throw BadRequestException("Payment cannot exceed remaining balance. Balance is $currentBalance")
        }

        // 2) Insert payment
        val paymentId = FamilyPaymentTable.insertAndGetId {

            it[FamilyPaymentTable.family_fee_record] = EntityID(family_fee_record, FamilyFeeRecordTable)

            it[FamilyPaymentTable.amount] = amount
            it[FamilyPaymentTable.date_created] = System.currentTimeMillis()
        }.value

        // 3) Update balances on family fee record
        val newPaid = alreadyPaid + amount
        val newBalance = amountToPay - newPaid
        val isFullyPaid = newBalance <= 0

        FamilyFeeRecordTable.update({ FamilyFeeRecordTable.id eq family_fee_record }) {
            it[amount_paid] = newPaid
            it[balance] = newBalance
            it[is_fully_paid] = isFullyPaid
        }

        // ✅ 4) NOW fetch wards (THIS is where your wards code goes)
        val wards = StudentsTable
            .join(
                AccountTable,
                JoinType.INNER,
                additionalConstraint = { StudentsTable.user eq AccountTable.id }
            )
            .selectAll()
            .where { StudentsTable.family eq familyId }
            .map { it[AccountTable.fullName] }
            .filter { it.isNotBlank() }

        // 5) Build response DTO
        // You already have FamilyFeeRecordMinimal; build it the same way you do elsewhere
        val ffrMinimal = recordRow.toFamilyFeeRecordMinimalAfterUpdate(
            amountPaid = newPaid,
            newBalance = newBalance,
            isFullyPaid = isFullyPaid)

        return@transaction FamilyPaymentResponseDto(
            id = paymentId,
            family_fee_record = ffrMinimal,
            amount = amount,
            date_created = System.currentTimeMillis(), // or read from inserted row if you store it
            balance = newBalance,
            wards = wards
        )
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
    ): FamilyPaymentResult = transaction {

        // 1️⃣ Fetch family fee record
        val record = FamilyFeeRecordTable
            .selectAll()
            .where { FamilyFeeRecordTable.id eq familyFeeRecordId }
            .singleOrNull()
            ?: throw BadRequestException("Family fee record not found")

        val currentBalance = record[FamilyFeeRecordTable.balance]

        if (amount <= 0) {
            throw BadRequestException("Payment amount must be greater than zero")
        }

        if (amount > currentBalance) {
            throw BadRequestException(
                "Payment cannot exceed remaining balance. Balance is $currentBalance"
            )
        }

        val amountToPay = record[FamilyFeeRecordTable.amount_to_pay]
        val alreadyPaid = record[FamilyFeeRecordTable.amount_paid]
        val newPaid = alreadyPaid + amount
        val newBalance = amountToPay - newPaid
        val isFullyPaid = newBalance <= 0

        // 2️⃣ Insert payment
        val paymentId = FamilyPaymentTable.insertAndGetId {
            it[family_fee_record] = familyFeeRecordId
            it[FamilyPaymentTable.amount] = amount
            it[FamilyPaymentTable.payment_method] = paymentMethod
            it[FamilyPaymentTable.date_created] = System.currentTimeMillis()
        }.value

        // 3️⃣ Update family fee record
        FamilyFeeRecordTable.update(
            { FamilyFeeRecordTable.id eq familyFeeRecordId }
        ) {
            it[amount_paid] = newPaid
            it[balance] = newBalance
            it[is_fully_paid] = isFullyPaid
        }

        // 4️⃣ Requery response DTO
        val responseDto = FamilyPaymentTable
            .join(
                FamilyFeeRecordTable,
                JoinType.INNER,
                FamilyPaymentTable.family_fee_record,
                FamilyFeeRecordTable.id
            )
            .join(
                FamilyTable,
                JoinType.LEFT,
                FamilyFeeRecordTable.family,
                FamilyTable.id
            )
            .join(
                TermTable,
                JoinType.LEFT,
                FamilyFeeRecordTable.term,
                TermTable.id
            )
            .join(
                AcademicYearTable,
                JoinType.LEFT,
                FamilyFeeRecordTable.academic_year,
                AcademicYearTable.id
            )
            .selectAll()
            .where { FamilyPaymentTable.id eq paymentId }
            .single()
            .toFamilyPaymentResponseDto()

        // ===========================
        // ✅ BUILD SMS + WARDS LIST
        // ===========================

        val familyId = record[FamilyFeeRecordTable.family]

        val studentRows = StudentsTable
            .join(
                AccountTable,
                JoinType.INNER,
                additionalConstraint = {
                    StudentsTable.user eq AccountTable.id
                }
            )
            .selectAll()
            .where { StudentsTable.family eq familyId }
            .toList()

        // ✅ Build wards list (FOR RECEIPT + RESPONSE)
        val wards = studentRows
            .map { it[AccountTable.fullName] }
            .filter { it.isNotBlank() }

        val studentNames = wards
            .joinToString(", ")
            .ifBlank { "your wards" }

        val parentPhone = studentRows
            .firstOrNull()
            ?.toStudentProfile()
            ?.contactOfFather

        val smsPayload = parentPhone?.let { phone ->
            val paymentStatus =
                if (newBalance > 0) "part payment" else "full payment"

            SmsPayload(
                phone = phone,
                message = """
                Dear Parent/Guardian,
                You made a $paymentStatus of GH₵ $amount for your wards: $studentNames.
                Purpose: School Fees
                Term/Year: ${responseDto.family_fee_record.term?.name} – ${responseDto.family_fee_record.academic_year?.name}.
                Balance: GH₵ $newBalance.
                Thank you.
            """.trimIndent()
            )
        }

        // ✅ Attach wards to response
        val responseWithWards = responseDto.copy(wards = wards)

// ==========================================================
// ✅ ✅ CREATE FAMILY RECEIPT (PDF/Receipt record)
// Place this RIGHT HERE (before returning)
// ==========================================================
        val receipt = FamilyReceiptRepository.createReceipt(
            familyPaymentId = paymentId,
            familyFeeRecordId = familyFeeRecordId,

            // family name (already inside the responseDto’s family_fee_record.family)
            familyName = responseDto.family_fee_record.family?.name ?: "Family",

            wards = wards,
            amountPaid = amount,
            balanceAfter = newBalance,
            paymentMethod = paymentMethod,

            termName = responseDto.family_fee_record.term?.name,
            academicYearName = responseDto.family_fee_record.academic_year?.name
        )

// ✅ Attach receipt to the response (ensure your DTO has `receipt: FamilyReceiptDto? = null`)
        val responseWithReceipt = responseWithWards.copy(receipt = receipt)

// ✅ Return with receipt included
        FamilyPaymentResult(
            response = responseWithReceipt,
            sms = smsPayload
        )
    }


    fun delete(id: Int): Boolean = transaction {
        FamilyPaymentTable.deleteWhere { FamilyPaymentTable.id eq id } > 0
    }
}











@Serializable
data class FamilyPaymentResult(
    val response: FamilyPaymentResponseDto,
    val sms: SmsPayload?
)