package com.example.fees.dtos.responses

import com.example.account.AccountTable
import com.example.fees.tables.PaymentTable
import com.example.fees.tables.StudentFeeRecordTable
import com.example.minimals.FeeStructureMinimal
import com.example.minimals.StudentFeeRecordMinimal
import com.example.student.StudentsTable
import io.ktor.http.cio.Response
import org.jetbrains.exposed.sql.ResultRow
import com.example.fees.tables.FeeStructureTable
import com.example.minimals.AcademicYearMinimal
import com.example.minimals.AccountNameOnly
import com.example.minimals.GradeClassMinimal
import com.example.minimals.StudentNameOnly
import com.example.minimals.TermMinimal
import com.example.student.tables.AcademicYearTable
import com.example.student.tables.NewGradeClassTable
import com.example.student.tables.TermTable

fun ResultRow.toPaymentResponseDto() = PaymentResponseDto(
    id = this[PaymentTable.id].value,
    amount = this[PaymentTable.amount],
    date_created = this[PaymentTable.date_created],
    balance = this[StudentFeeRecordTable.balance],



    student_fee_record = StudentFeeRecordMinimal(
        id = this[StudentFeeRecordTable.id].value,
        student = StudentNameOnly(
            id = this[StudentsTable.id].value,
            user = AccountNameOnly(
                fullName = this[AccountTable.fullName],
            ),
        ),

        feeStructure = FeeStructureMinimal(
            id = this[FeeStructureTable.id].value,
            academic_year = AcademicYearMinimal(
                name = this[AcademicYearTable.name],
            ),
            term = TermMinimal(
                name = this[TermTable.name],
            ),
            grade_class = GradeClassMinimal(
                name = this[NewGradeClassTable.name],
            ),

        ),


    )
)
