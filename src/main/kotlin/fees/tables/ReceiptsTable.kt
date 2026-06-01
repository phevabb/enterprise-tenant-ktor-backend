package com.example.fees.tables






import com.example.student.StudentsTable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption

object ReceiptsTable : IntIdTable("receipts") {

    val receiptNo = varchar("receipt_no", 40).uniqueIndex()

    val payment = reference("payment_id", PaymentTable, onDelete = ReferenceOption.CASCADE)
    val studentFeeRecord = reference("student_fee_record_id", StudentFeeRecordTable, onDelete = ReferenceOption.CASCADE)
    val student = reference("student_id", StudentsTable)

    val amountPaid = integer("amount_paid")
    val balanceAfter = integer("balance_after")

    val paymentMethod = varchar("payment_method", 20).nullable()

    // snapshot fields for printing (so receipt does not change later)
    val studentName = varchar("student_name", 222)
    val className = varchar("class_name", 100)
    val termName = varchar("term_name", 50)
    val academicYearName = varchar("academic_year_name", 50)

    val createdAt = long("created_at")
}
