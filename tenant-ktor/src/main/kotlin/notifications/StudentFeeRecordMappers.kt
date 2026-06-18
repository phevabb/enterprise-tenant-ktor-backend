package com.example.notifications



import com.example.fees.tables.StudentFeeRecordTable
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toStudentFeeRecordSnapshotDto() = StudentFeeRecordSnapshotDto(
    id = this[StudentFeeRecordTable.id].value,
    studentId = this[StudentFeeRecordTable.student].value,
    feeStructureId = this[StudentFeeRecordTable.feeStructure].value,
    amountPaid = this[StudentFeeRecordTable.amountPaid],
    balance = this[StudentFeeRecordTable.balance],
    isFullyPaid = this[StudentFeeRecordTable.isFullyPaid],
    dateCreated = this[StudentFeeRecordTable.dateCreated]
)