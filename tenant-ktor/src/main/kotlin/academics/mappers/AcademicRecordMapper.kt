package com.example.academics.mappers


import com.example.student.tables.AcademicYearTable
import com.example.student.StudentsTable
import com.example.student.tables.TermTable
import com.example.academics.dtos.response.AcademicRecordWithScoresResponse
import com.example.academics.tables.AcademicRecordsTable
import com.example.account.AccountTable
import com.example.minimals.AcademicYearMinimal
import com.example.minimals.ComplexStudentMinimalDto
import com.example.minimals.GradeClassMinimal
import com.example.minimals.StudentMinimalDto
import com.example.minimals.TermMinimal
import com.example.student.tables.NewGradeClassTable
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toAcademicRecord() = AcademicRecordWithScoresResponse(
    id = this[AcademicRecordsTable.id].value,

    student =
        ComplexStudentMinimalDto(
            id = this[StudentsTable.id].value,
            name = this[AccountTable.fullName],  // ✅ correct source
            userId = this[AccountTable.userId],
        ),
    term = TermMinimal(
        name = this[TermTable.name]
    ),
    academicYear = AcademicYearMinimal(
        name = this[AcademicYearTable.name]
    ),

    gradeClass = GradeClassMinimal(
        name = this[NewGradeClassTable.name]

    ),

    overallPosition = this[AcademicRecordsTable.overallPosition],
    attendance = this[AcademicRecordsTable.attendance],
    numberOnRoll = this[AcademicRecordsTable.numberOnRoll],

    conduct = this[AcademicRecordsTable.conduct],
    interest = this[AcademicRecordsTable.interest],
    attitude = this[AcademicRecordsTable.attitude],
    teacherRemarks = this[AcademicRecordsTable.teacherRemarks],
    headTeacherRemarks = this[AcademicRecordsTable.headTeacherRemarks],
    nextTermBegins = this[AcademicRecordsTable.nextTermBegins],
    promotedTo = this[AcademicRecordsTable.promotedTo],

    rawScoreTotal = this[AcademicRecordsTable.rawScoreTotal]
)