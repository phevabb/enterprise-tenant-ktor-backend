package com.example.fees.mappers


import com.example.fees.models.StudentFeeRecordModel
import com.example.fees.tables.StudentFeeRecordTable
import org.jetbrains.exposed.sql.ResultRow
import com.example.student.models.*
import com.example.student.StudentsTable
import com.example.student.tables.*
import com.example.account.AccountTable
import com.example.account.Account
import com.example.fees.models.FeeStructureModel
import com.example.fees.tables.FeeStructureTable
import com.example.minimals.AcademicYearMinimal
import com.example.minimals.AccountNameOnly
import com.example.minimals.FeeStructureMinimal
import com.example.minimals.GradeClassMinimal
import com.example.minimals.StudentNameOnly
import com.example.minimals.TermMinimal


fun ResultRow.toStudentFeeRecordModel() = StudentFeeRecordModel(
    id = this[StudentFeeRecordTable.id].value,
//    studentId = this[StudentFeeRecordTable.student].value,
//    feeStructureId = this[StudentFeeRecordTable.feeStructure].value,

    student = StudentNameOnly(
        id = this[StudentsTable.id].value,
        user = AccountNameOnly(
            fullName = this[AccountTable.fullName],

        )
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
            )

        ),



    amountPaid = this[StudentFeeRecordTable.amountPaid],
    balance = this[StudentFeeRecordTable.balance],
    isFullyPaid = this[StudentFeeRecordTable.isFullyPaid],
    dateCreated = this[StudentFeeRecordTable.dateCreated]
)