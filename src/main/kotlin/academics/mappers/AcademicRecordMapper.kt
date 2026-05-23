package com.example.academics.mappers





import com.example.academics.dtos.response.AcademicRecordResponse
import com.example.academics.tables.AcademicRecordsTable
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toAcademicRecord() = AcademicRecordResponse(
    id = this[AcademicRecordsTable.id].value,
    studentId = this[AcademicRecordsTable.student].value,
    termId = this[AcademicRecordsTable.term].value,
    academicYearId = this[AcademicRecordsTable.academicYear].value,
    classLevelId = this[AcademicRecordsTable.classLevel].value,

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