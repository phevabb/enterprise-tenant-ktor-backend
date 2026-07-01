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
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

object FamilyPaymentRepository {

    private fun Transaction.setTenantSchema(tenantSchema: String) {
        val safeSchema = tenantSchema.replace("\"", "\"\"")
        exec("""SET LOCAL search_path TO "$safeSchema"""")
    }


    fun create(
        tenantSchema: String,
        family_fee_record: Int,
        amount: Int,
    ): FamilyPaymentResponseDto = transaction {

        setTenantSchema(tenantSchema)

        val recordRow = FamilyFeeRecordTable
            .selectAll()
            .where { FamilyFeeRecordTable.id eq family_fee_record }
            .singleOrNull()
            ?: throw BadRequestException("Family fee record not found")

        val familyId = recordRow[FamilyFeeRecordTable.family]
        val currentBalance = recordRow[FamilyFeeRecordTable.balance]
        val amountToPay = recordRow[FamilyFeeRecordTable.amount_to_pay]
        val alreadyPaid = recordRow[FamilyFeeRecordTable.amount_paid]

        if (amount <= 0)
            throw BadRequestException("Payment amount must be greater than zero")

        if (amount > currentBalance)
            throw BadRequestException(
                "Payment cannot exceed remaining balance. Balance is $currentBalance"
            )

        val paymentId = FamilyPaymentTable.insertAndGetId {

            it[FamilyPaymentTable.family_fee_record] =
                EntityID(family_fee_record, FamilyFeeRecordTable)

            it[FamilyPaymentTable.amount] = amount
            it[FamilyPaymentTable.date_created] = System.currentTimeMillis()
        }.value

        val newPaid = alreadyPaid + amount
        val newBalance = amountToPay - newPaid
        val isFullyPaid = newBalance <= 0

        FamilyFeeRecordTable.update(
            { FamilyFeeRecordTable.id eq family_fee_record }
        ) {
            it[amount_paid] = newPaid
            it[balance] = newBalance
            it[is_fully_paid] = isFullyPaid
        }

        val wards = StudentsTable
            .join(
                AccountTable,
                JoinType.INNER,
                additionalConstraint = {
                    StudentsTable.user eq AccountTable.id
                }
            )
            .selectAll()
            .where { StudentsTable.family eq familyId }
            .map { it[AccountTable.fullName] }
            .filter { it.isNotBlank() }

        val ffrMinimal = recordRow.toFamilyFeeRecordMinimalAfterUpdate(
            amountPaid = newPaid,
            newBalance = newBalance,
            isFullyPaid = isFullyPaid
        )

        FamilyPaymentResponseDto(
            id = paymentId,
            family_fee_record = ffrMinimal,
            amount = amount,
            date_created = System.currentTimeMillis(),
            balance = newBalance,
            wards = wards
        )
    }

    fun findById(
        tenantSchema: String,
        id: Int
    ): FamilyPaymentResponseDto? = transaction {

        setTenantSchema(tenantSchema)

        FamilyPaymentTable
            .join(
                FamilyFeeRecordTable,
                JoinType.INNER,
                FamilyPaymentTable.family_fee_record,
                FamilyFeeRecordTable.id
            )
            .join(
                FamilyTable,
                JoinType.INNER,
                FamilyFeeRecordTable.family,
                FamilyTable.id
            )
            .selectAll()
            .where { FamilyPaymentTable.id eq id }
            .singleOrNull()
            ?.toFamilyPaymentResponseDto()
    }

    fun findAll(
        tenantSchema: String
    ): List<FamilyPaymentResponseDto> = transaction {

        setTenantSchema(tenantSchema)

        FamilyPaymentTable
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
            .orderBy(FamilyPaymentTable.id, SortOrder.DESC)
            .map { row ->
                row.toFamilyPaymentResponseDto()
            }
    }


    fun createPaymentAndUpdateFfr(
        tenantSchema: String,
        familyFeeRecordId: Int,
        schoolName: String,
        amount: Int,
        paymentMethod: String
    ): FamilyPaymentResult = transaction {

        println("DEBUG createPaymentAndUpdateFfr started")
        println("DEBUG tenantSchema = $tenantSchema")
        println("DEBUG familyFeeRecordId = $familyFeeRecordId")
        println("DEBUG amount = $amount")
        println("DEBUG paymentMethod = $paymentMethod")

        println("DEBUG setting tenant schema in FamilyPaymentRepository...")
        setTenantSchema(tenantSchema)
        println("DEBUG tenant schema set successfully in FamilyPaymentRepository")

        // 1️⃣ Fetch family fee record
        println("DEBUG fetching family fee record...")

        val record = FamilyFeeRecordTable
            .selectAll()
            .where { FamilyFeeRecordTable.id eq familyFeeRecordId }
            .singleOrNull()
            ?: throw BadRequestException("Family fee record not found")

        println("DEBUG family fee record found")

        val currentBalance = record[FamilyFeeRecordTable.balance]

        println("DEBUG currentBalance = $currentBalance")

        if (amount <= 0) {
            println("DEBUG invalid amount: $amount")
            throw BadRequestException("Payment amount must be greater than zero")
        }

        if (amount > currentBalance) {
            println("DEBUG amount exceeds balance. amount=$amount, currentBalance=$currentBalance")

            throw BadRequestException(
                "Payment cannot exceed remaining balance. Balance is $currentBalance"
            )
        }

        val amountToPay = record[FamilyFeeRecordTable.amount_to_pay]
        val alreadyPaid = record[FamilyFeeRecordTable.amount_paid]
        val newPaid = alreadyPaid + amount
        val newBalance = amountToPay - newPaid
        val isFullyPaid = newBalance <= 0

        println("DEBUG amountToPay = $amountToPay")
        println("DEBUG alreadyPaid = $alreadyPaid")
        println("DEBUG newPaid = $newPaid")
        println("DEBUG newBalance = $newBalance")
        println("DEBUG isFullyPaid = $isFullyPaid")

        // 2️⃣ Insert payment
        println("DEBUG inserting payment record...")

        val paymentId = FamilyPaymentTable.insertAndGetId {
            it[family_fee_record] = familyFeeRecordId
            it[FamilyPaymentTable.amount] = amount
            it[FamilyPaymentTable.payment_method] = paymentMethod
            it[FamilyPaymentTable.date_created] = System.currentTimeMillis()
        }.value

        println("DEBUG payment inserted successfully. paymentId = $paymentId")

        // 3️⃣ Update family fee record
        println("DEBUG updating family fee record...")

        FamilyFeeRecordTable.update(
            { FamilyFeeRecordTable.id eq familyFeeRecordId }
        ) {
            it[amount_paid] = newPaid
            it[balance] = newBalance
            it[is_fully_paid] = isFullyPaid
        }

        println("DEBUG family fee record updated successfully")

        // 4️⃣ Requery response DTO
        println("DEBUG querying response DTO...")

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

        println("DEBUG response DTO created successfully")

        // ===========================
        // ✅ BUILD SMS + WARDS LIST
        // ===========================

        val familyId = record[FamilyFeeRecordTable.family]

        println("DEBUG familyId = $familyId")
        println("DEBUG fetching wards/students for family...")

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

        println("DEBUG studentRows count = ${studentRows.size}")

        // ✅ Build wards list (FOR RECEIPT + RESPONSE)
        val wards = studentRows
            .map { it[AccountTable.fullName] }
            .filter { it.isNotBlank() }

        println("DEBUG wards = $wards")

        val studentNames = wards
            .joinToString(", ")
            .ifBlank { "your wards" }

        println("DEBUG studentNames = $studentNames")

        val parentPhone = studentRows
            .firstOrNull()
            ?.toStudentProfile()
            ?.contactOfFather

        println("DEBUG parentPhone = $parentPhone")

        val smsPayload = parentPhone?.let { phone ->
            val paymentStatus =
                if (newBalance > 0) "part payment" else "full payment"

            println("DEBUG building SMS payload. paymentStatus = $paymentStatus")

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

        println("DEBUG smsPayload created = ${smsPayload != null}")

        // ✅ Attach wards to response
        val responseWithWards = responseDto.copy(wards = wards)

        println("DEBUG responseWithWards created")

        // ==========================================================
        // ✅ ✅ CREATE FAMILY RECEIPT
        // ==========================================================

        println("DEBUG about to create family receipt...")
        println("DEBUG calling FamilyReceiptRepository.createReceipt")
        println("DEBUG receipt tenantSchema = $tenantSchema")
        println("DEBUG receipt paymentId = $paymentId")
        println("DEBUG receipt familyFeeRecordId = $familyFeeRecordId")
        println("DEBUG receipt familyName = ${responseDto.family_fee_record.family?.name ?: "Family"}")
        println("DEBUG receipt wards = $wards")
        println("DEBUG receipt amountPaid = $amount")
        println("DEBUG receipt balanceAfter = $newBalance")
        println("DEBUG receipt paymentMethod = $paymentMethod")
        println("DEBUG receipt termName = ${responseDto.family_fee_record.term?.name}")
        println("DEBUG receipt academicYearName = ${responseDto.family_fee_record.academic_year?.name}")

        val receipt = FamilyReceiptRepository.createReceipt(
            tenantSchema = tenantSchema,
            familyPaymentId = paymentId,
            familyFeeRecordId = familyFeeRecordId,
            schoolName = schoolName,

            familyName = responseDto.family_fee_record.family?.name ?: "Family",

            wards = wards,
            amountPaid = amount,
            balanceAfter = newBalance,
            paymentMethod = paymentMethod,

            termName = responseDto.family_fee_record.term?.name,
            academicYearName = responseDto.family_fee_record.academic_year?.name
        )

        println("DEBUG receipt created successfully")
        println("DEBUG receipt = $receipt")

        // ✅ Attach receipt to the response
        val responseWithReceipt = responseWithWards.copy(receipt = receipt)

        println("DEBUG responseWithReceipt created")
        println("DEBUG createPaymentAndUpdateFfr completed successfully")

        FamilyPaymentResult(
            response = responseWithReceipt,
            sms = smsPayload
        )
    }


    fun delete(
        tenantSchema: String,
        id: Int
    ): Boolean = transaction {

        setTenantSchema(tenantSchema)

        FamilyPaymentTable.deleteWhere {
            FamilyPaymentTable.id eq id
        } > 0
    }



}











@Serializable
data class FamilyPaymentResult(
    val response: FamilyPaymentResponseDto,
    val sms: SmsPayload?
)