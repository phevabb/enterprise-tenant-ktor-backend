package com.example.fees.mappers


import com.example.fees.models.StudentFeeRecordModel
import com.example.fees.tables.StudentFeeRecordTable
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toStudentFeeRecordModel() = StudentFeeRecordModel(
    id = this[StudentFeeRecordTable.id].value,
    studentId = this[StudentFeeRecordTable.student].value,
    feeStructureId = this[StudentFeeRecordTable.feeStructure].value,
    amountPaid = this[StudentFeeRecordTable.amountPaid],
    balance = this[StudentFeeRecordTable.balance],
    isFullyPaid = this[StudentFeeRecordTable.isFullyPaid],
    dateCreated = this[StudentFeeRecordTable.dateCreated]
)