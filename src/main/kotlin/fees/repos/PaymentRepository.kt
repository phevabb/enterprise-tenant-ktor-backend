package com.example.fees.repos


import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.example.fees.services.FeeRecordService
import com.example.notifications.SmsTemplates
import org.jetbrains.exposed.sql.*
import com.example.account.AccountTable
import com.example.account.toAccount
import com.example.fees.dtos.responses.PaymentResponseDto
import com.example.fees.dtos.responses.toPaymentResponseDto
import com.example.fees.mappers.toFeeStructureModel
import com.example.notifications.SmsPayload
import com.example.fees.tables.FeeStructureTable
import com.example.fees.tables.PaymentTable
import com.example.fees.tables.StudentFeeRecordTable
import com.example.notifications.toStudentFeeRecordSnapshotDto
import com.example.student.StudentsTable
import com.example.student.mappers.toAcademicYearModel
import com.example.student.mappers.toNewGradeClassModel
import com.example.student.mappers.toStudentProfile
import com.example.student.mappers.toTermModel
import com.example.student.tables.AcademicYearTable
import com.example.student.tables.NewGradeClassTable
import com.example.student.tables.TermTable
import io.ktor.server.plugins.BadRequestException
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
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


    fun delete(id: Int): Boolean = transaction{
        PaymentTable.deleteWhere{ PaymentTable.id eq id } > 0
    }


    fun createPaymentAndUpdateSfr(
        studentFeeRecordId: Int,
        amount: Int,
        paymentMethod: String?
    ): CreatePaymentResult = transaction {

        val sfr = StudentFeeRecordTable
            .selectAll()
            .where { StudentFeeRecordTable.id eq studentFeeRecordId }
            .singleOrNull()
            ?: throw BadRequestException("Student fee record not found")

        val balance = sfr[StudentFeeRecordTable.balance]

        if (amount <= 0)
            throw BadRequestException("Payment amount must be greater than zero")

        if (amount > balance)
            throw BadRequestException(
                "Payment exceeds remaining balance. Balance left: GH₵ $balance"
            )

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


        val studentId = dto.studentId
        val feeId = dto.feeStructureId



        val studentObject = StudentsTable
            .selectAll()
            .where { StudentsTable.id eq studentId }
            .singleOrNull()
            ?: throw BadRequestException("Student  not found")
        val student_json_dto = studentObject.toStudentProfile()


        val user_id = student_json_dto.user
        val con_of_dad = student_json_dto.contactOfFather


        val user_object = AccountTable
            .selectAll()
            .where { AccountTable.id eq user_id }
            .singleOrNull()
            ?: throw BadRequestException("Student  not found")


        val final_user = user_object.toAccount()
        val full_name = final_user.fullName





        val feeStructureObject = FeeStructureTable
            .selectAll()
            .where { FeeStructureTable.id eq feeId }
            .singleOrNull()
            ?: throw BadRequestException("Fee structure not found")
        val seeStructure_dto = feeStructureObject.toFeeStructureModel()

        val academic = seeStructure_dto.academicYearId
        val year = AcademicYearTable
            .selectAll()
            .where { AcademicYearTable.id eq academic }
            .singleOrNull()
            ?: throw BadRequestException("Year not found")
        val year_dto = year.toAcademicYearModel()
        val year_name = year_dto.name
        println("yearrrrrrrrrrrrrr is $year_name")



        val term    = seeStructure_dto.termId
        val trueTerm = TermTable
            .selectAll()
            .where { TermTable.id eq term }
            .singleOrNull()
            ?: throw BadRequestException("Term not found")
        val term_dto = trueTerm.toTermModel()
        val term_name = term_dto.name


        val gradeclass = seeStructure_dto.gradeClassId
        val Geade_class = NewGradeClassTable
            .selectAll()
            .where { NewGradeClassTable.id eq gradeclass }
            .singleOrNull()
            ?: throw BadRequestException("Class not found")
        val class_dto = Geade_class.toNewGradeClassModel()
        val class_name = class_dto.name





        // ✅ Father only
        // ====== TEST SMS (hardcoded) ======
        val fatherPhone = con_of_dad


        val smsPayload = SmsPayload(
            phone = fatherPhone,
            message = SmsTemplates.paymentReceived(
                studentName = full_name,
                amountPaid = amount,
                balance = true_balance,              // <-- fake balance for testing
                className = class_name,       // <-- fake class
                term = term_name,            // <-- fake term
                academicYear = year_name  // <-- fake year
            )
        )

        CreatePaymentResult(
            response = findById(paymentId)!!,
            sms = smsPayload
        )
    }


    fun applyPaymentDelta(sfrId: Int, delta: Int) {
        require(delta >= 0) { "delta must be >= 0 for payments. Use a separate function for refunds/reversals." }

        StudentFeeRecordTable.update({ StudentFeeRecordTable.id eq sfrId }) { ub ->
            with(SqlExpressionBuilder) {
                val bal = StudentFeeRecordTable.balance
                val paid = StudentFeeRecordTable.amountPaid

                // newBalance = max(balance - delta, 0)
                val newBalanceExpr =
                    Case()
                        .When((bal - delta) less 0, intLiteral(0))
                        .Else(bal - delta)

                // applied = min(delta, balance)
                // If (balance - delta) < 0 => delta > balance => applied = balance
                // else applied = delta
                val appliedExpr =
                    Case()
                        .When((bal - delta) less 0, bal)
                        .Else(intLiteral(delta))

                // amountPaid = amountPaid + applied
                ub.update(StudentFeeRecordTable.amountPaid, paid + appliedExpr)

                // balance = newBalance (never negative)
                ub.update(StudentFeeRecordTable.balance, newBalanceExpr)

                // isFullyPaid = (newBalance <= 0)  (same as == 0 after clamp)
                ub.update(
                    StudentFeeRecordTable.isFullyPaid,
                    Case()
                        .When(newBalanceExpr lessEq 0, booleanLiteral(true))
                        .Else(booleanLiteral(false))
                )
            }
        }
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

