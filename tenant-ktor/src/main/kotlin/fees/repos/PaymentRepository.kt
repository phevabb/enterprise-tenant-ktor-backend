package com.example.fees.repos


import com.example.account.AccountTable
import com.example.account.toAccount
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.SqlExpressionBuilder.between
import org.jetbrains.exposed.sql.transactions.transaction


import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import com.example.fees.dtos.responses.PaymentResponseDto
import com.example.fees.dtos.responses.toPaymentResponseDto
import com.example.fees.mappers.toFeeStructureModel
import com.example.fees.services.FeeRecordService
import com.example.fees.tables.FeeStructureTable
import com.example.fees.tables.PaymentTable
import com.example.fees.tables.StudentFeeRecordTable
import com.example.notifications.SmsPayload
import com.example.notifications.SmsTemplates
import com.example.notifications.toStudentFeeRecordSnapshotDto
import com.example.student.StudentsTable
import com.example.student.mappers.toAcademicYearModel
import com.example.student.mappers.toNewGradeClassModel
import com.example.student.mappers.toStudentProfile
import com.example.student.mappers.toTermModel
import com.example.student.tables.AcademicYearTable
import com.example.student.tables.NewGradeClassTable
import com.example.student.tables.TermTable
import io.ktor.server.plugins.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

import java.time.*
import java.time.temporal.TemporalAdjusters

private fun dateRangeForFilterMillis(
    dateFilter: String?,
    zoneId: ZoneId = ZoneId.systemDefault() // set ZoneId.of("UTC") if you store UTC
): Pair<Long, Long>? {

    val today = LocalDate.now(zoneId)

    fun toMillis(dt: LocalDateTime): Long =
        dt.atZone(zoneId).toInstant().toEpochMilli()

    return when (dateFilter?.lowercase()) {
        "today" -> {
            val start = today.atStartOfDay()
            val end = today.atTime(LocalTime.MAX)
            toMillis(start) to toMillis(end)
        }
        "7days" -> {
            val start = today.minusDays(6).atStartOfDay()  // inclusive last 7 days
            val end = today.atTime(LocalTime.MAX)
            toMillis(start) to toMillis(end)
        }
        "month" -> {
            val start = today.withDayOfMonth(1).atStartOfDay()
            val end = today.with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX)
            toMillis(start) to toMillis(end)
        }
        "year" -> {
            val start = today.withDayOfYear(1).atStartOfDay()
            val end = today.with(TemporalAdjusters.lastDayOfYear()).atTime(LocalTime.MAX)
            toMillis(start) to toMillis(end)
        }
        else -> null
    }
}
object PaymentRepository {

    private fun Transaction.setTenantSchema(tenantSchema: String) {
        val safeSchema = tenantSchema.replace("\"", "\"\"")
        exec("""SET LOCAL search_path TO "$safeSchema"""")
    }

    fun create(
        tenantSchema: String,
        student_fee_record: Int,
        amount: Int,
    ) = transaction {

        setTenantSchema(tenantSchema)

        val id = PaymentTable.insertAndGetId {
            it[PaymentTable.student_fee_record] = EntityID(student_fee_record, StudentFeeRecordTable)
            it[PaymentTable.amount] = amount
            it[date_created] = System.currentTimeMillis()
        }.value

        findById(tenantSchema, id) ?: error("$id not found")
    }

    fun findById(
        tenantSchema: String,
        id: Int
    ): PaymentResponseDto? = transaction {

        setTenantSchema(tenantSchema)

        PaymentTable
            .join(StudentFeeRecordTable, JoinType.INNER, PaymentTable.student_fee_record, StudentFeeRecordTable.id)
            .join(StudentsTable, JoinType.INNER, StudentFeeRecordTable.student, StudentsTable.id)
            .join(AccountTable, JoinType.INNER, StudentsTable.user, AccountTable.id)
            .join(FeeStructureTable, JoinType.INNER, StudentFeeRecordTable.feeStructure, FeeStructureTable.id)
            .join(NewGradeClassTable, JoinType.INNER, FeeStructureTable.grade_class, NewGradeClassTable.id)
            .join(AcademicYearTable, JoinType.INNER, FeeStructureTable.academic_year, AcademicYearTable.id)
            .join(TermTable, JoinType.INNER, FeeStructureTable.term, TermTable.id)
            .selectAll()
            .where { PaymentTable.id eq id }
            .singleOrNull()
            ?.toPaymentResponseDto()
    }

    fun findAll(tenantSchema: String): List<PaymentResponseDto> = transaction {

        setTenantSchema(tenantSchema)

        PaymentTable
            .join(StudentFeeRecordTable, JoinType.INNER, PaymentTable.student_fee_record, StudentFeeRecordTable.id)
            .join(StudentsTable, JoinType.INNER, StudentFeeRecordTable.student, StudentsTable.id)
            .join(AccountTable, JoinType.INNER, StudentsTable.user, AccountTable.id)
            .join(FeeStructureTable, JoinType.INNER, StudentFeeRecordTable.feeStructure, FeeStructureTable.id)
            .join(NewGradeClassTable, JoinType.INNER, FeeStructureTable.grade_class, NewGradeClassTable.id)
            .join(AcademicYearTable, JoinType.INNER, FeeStructureTable.academic_year, AcademicYearTable.id)
            .join(TermTable, JoinType.INNER, FeeStructureTable.term, TermTable.id)
            .selectAll()
            .orderBy(PaymentTable.id, SortOrder.DESC)
            .map { it.toPaymentResponseDto() }
    }

    fun findAllPaginated(
        tenantSchema: String,
        page: Int,
        limit: Int,
        search: String?,
        dateFilter: String?
    ): Pair<List<PaymentResponseDto>, Long> = transaction {

        setTenantSchema(tenantSchema)

        val safePage = if (page < 1) 1 else page
        val safeLimit = if (limit < 1) 20 else limit
        val offset = ((safePage - 1) * safeLimit).toLong()

        val baseQuery = PaymentTable
            .join(StudentFeeRecordTable, JoinType.INNER, PaymentTable.student_fee_record, StudentFeeRecordTable.id)
            .join(StudentsTable, JoinType.INNER, StudentFeeRecordTable.student, StudentsTable.id)
            .join(AccountTable, JoinType.INNER, StudentsTable.user, AccountTable.id)
            .join(FeeStructureTable, JoinType.INNER, StudentFeeRecordTable.feeStructure, FeeStructureTable.id)
            .join(NewGradeClassTable, JoinType.INNER, FeeStructureTable.grade_class, NewGradeClassTable.id)
            .join(AcademicYearTable, JoinType.INNER, FeeStructureTable.academic_year, AcademicYearTable.id)
            .join(TermTable, JoinType.INNER, FeeStructureTable.term, TermTable.id)
            .selectAll()

        val range = dateRangeForFilterMillis(dateFilter)

        val withDateFilter = if (range != null) {
            val (startMs, endMs) = range
            baseQuery.andWhere { PaymentTable.date_created.between(startMs, endMs) }
        } else baseQuery

        val finalQuery = if (!search.isNullOrBlank()) {
            val pattern = "%${search.lowercase()}%"
            withDateFilter.andWhere {
                (AccountTable.fullName.lowerCase() like pattern) or
                        (NewGradeClassTable.name.lowerCase() like pattern) or
                        (AcademicYearTable.name.lowerCase() like pattern) or
                        (TermTable.name.lowerCase() like pattern)
            }
        } else withDateFilter

        val total = finalQuery.count()

        val items = finalQuery
            .orderBy(PaymentTable.id, SortOrder.DESC)
            .limit(safeLimit)
            .offset(offset)
            .map { it.toPaymentResponseDto() }

        Pair(items, total)
    }

    fun delete(
        tenantSchema: String,
        id: Int
    ): Boolean = transaction {

        setTenantSchema(tenantSchema)

        PaymentTable.deleteWhere { PaymentTable.id eq id } > 0
    }

    fun createPaymentAndUpdateSfr(
        tenantSchema: String,
        studentFeeRecordId: Int,
        amount: Int,
        paymentMethod: String?
    ): CreatePaymentResult = transaction {

        setTenantSchema(tenantSchema)

        val sfr = StudentFeeRecordTable
            .selectAll()
            .where { StudentFeeRecordTable.id eq studentFeeRecordId }
            .singleOrNull()
            ?: throw BadRequestException("Student fee record not found")

        val balance = sfr[StudentFeeRecordTable.balance]

        if (amount <= 0)
            throw BadRequestException("Payment amount must be greater than zero")

        if (amount > balance)
            throw BadRequestException("Payment exceeds remaining balance. Balance left: GH₵ $balance")

        val paymentId = PaymentTable.insertAndGetId {
            it[student_fee_record] = EntityID(studentFeeRecordId, StudentFeeRecordTable)
            it[PaymentTable.amount] = amount
            it[PaymentTable.payment_method] = paymentMethod
            it[PaymentTable.date_created] = System.currentTimeMillis()
        }.value

        FeeRecordService.applyPaymentDelta(studentFeeRecordId, amount)

        val updated = StudentFeeRecordTable
            .selectAll()
            .where { StudentFeeRecordTable.id eq studentFeeRecordId }
            .single()

        val dto = updated.toStudentFeeRecordSnapshotDto()
        val true_balance = dto.balance

        val studentObject = StudentsTable
            .selectAll()
            .where { StudentsTable.id eq dto.studentId }
            .singleOrNull()
            ?: throw BadRequestException("Student not found")

        val student_json_dto = studentObject.toStudentProfile()

        val user_object = AccountTable
            .selectAll()
            .where { AccountTable.id eq student_json_dto.user }
            .singleOrNull()
            ?: throw BadRequestException("Student user not found")

        val full_name = user_object.toAccount().fullName

        val feeStructureObject = FeeStructureTable
            .selectAll()
            .where { FeeStructureTable.id eq dto.feeStructureId }
            .singleOrNull()
            ?: throw BadRequestException("Fee structure not found")

        val seeStructure_dto = feeStructureObject.toFeeStructureModel()

        val year_name = AcademicYearTable
            .selectAll()
            .where { AcademicYearTable.id eq seeStructure_dto.academicYearId }
            .single()[AcademicYearTable.name]

        val term_name = TermTable
            .selectAll()
            .where { TermTable.id eq seeStructure_dto.termId }
            .single()[TermTable.name]

        val class_name = NewGradeClassTable
            .selectAll()
            .where { NewGradeClassTable.id eq seeStructure_dto.gradeClassId }
            .single()[NewGradeClassTable.name]

        val smsPayload = SmsPayload(
            phone = student_json_dto.contactOfFather,
            message = SmsTemplates.paymentReceived(
                studentName = full_name,
                amountPaid = amount,
                balance = true_balance,
                className = class_name,
                term = term_name,
                academicYear = year_name
            )
        )

        val receiptDto = ReceiptRepository.createReceipt(
            tenantSchema = tenantSchema,
            paymentId = paymentId,
            studentFeeRecordId = studentFeeRecordId,
            studentId = dto.studentId,
            studentName = full_name,
            className = class_name,
            termName = term_name,
            academicYearName = year_name,
            amountPaid = amount,
            balanceAfter = true_balance,
            paymentMethod = paymentMethod
        )

        val paymentResponse = findById(tenantSchema, paymentId)!!

        CreatePaymentResult(
            response = paymentResponse.copy(receipt = receiptDto),
            sms = smsPayload
        )
    }
}





val smsRow = (StudentFeeRecordTable
    .innerJoin(StudentsTable) // SFR.student_id -> Students.id
        // add other joins if you need them in the sms message:
        // .innerJoin(AccountTable)
        // .innerJoin(FeeStructureTable)
        // .innerJoin(NewGradeClassTable)
        // .innerJoin(TermTable)
        // .innerJoin(AcademicYearTable)
        )
    .selectAll()
        .where {StudentFeeRecordTable.id eq StudentFeeRecordTable.id}

    .singleOrNull()

val fatherPhone = smsRow?.get(StudentsTable.contactOfFather)

@Serializable
data class CreatePaymentResult(
    val response: PaymentResponseDto,


    val sms: SmsPayload?
)

