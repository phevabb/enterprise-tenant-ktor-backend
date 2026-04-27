package com.example.fees.tables

import com.example.student.StudentsTable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption

object PaymentTable: IntIdTable("payment") {

    val student_fee_record = reference("student_fee_record", StudentFeeRecordTable, onDelete = ReferenceOption.CASCADE)
    val payment_method = varchar("payment_method", 255).nullable()
    val amount = integer("amount")
    val date_created = long("date_created")
}