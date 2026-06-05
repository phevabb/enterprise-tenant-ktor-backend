package com.example.fees.repos



import com.example.fees.dtos.responses.ReceiptResponse
import com.example.fees.tables.ReceiptsTable
import com.example.fees.tables.PaymentTable
import com.example.fees.tables.StudentFeeRecordTable
import com.example.student.StudentsTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object ReceiptRepository {

    private fun Transaction.setTenantSchema(tenantSchema: String) {
        val safeSchema = tenantSchema.replace("\"", "\"\"")
        exec("""SET LOCAL search_path TO "$safeSchema"""")
    }

    private fun generateReceiptNo(): String {
        val date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
        val rand = (100000..999999).random()
        return "PSS-$date-$rand"
    }

    fun createReceipt(
        tenantSchema: String,
        paymentId: Int,
        studentFeeRecordId: Int,
        studentId: Int,
        studentName: String,
        className: String,
        termName: String,
        academicYearName: String,
        amountPaid: Int,
        balanceAfter: Int,
        paymentMethod: String?
    ): ReceiptResponse = transaction {

        setTenantSchema(tenantSchema)

        val receiptNo = generateReceiptNo()

        val receiptId = ReceiptsTable.insertAndGetId {
            it[ReceiptsTable.receiptNo] = receiptNo
            it[payment] = EntityID(paymentId, PaymentTable)
            it[studentFeeRecord] = EntityID(studentFeeRecordId, StudentFeeRecordTable)
            it[student] = EntityID(studentId, StudentsTable)

            it[ReceiptsTable.amountPaid] = amountPaid
            it[ReceiptsTable.balanceAfter] = balanceAfter
            it[ReceiptsTable.paymentMethod] = paymentMethod

            it[ReceiptsTable.studentName] = studentName
            it[ReceiptsTable.className] = className
            it[ReceiptsTable.termName] = termName
            it[ReceiptsTable.academicYearName] = academicYearName

            it[createdAt] = System.currentTimeMillis()
        }.value

        ReceiptResponse(
            id = receiptId,
            receiptNo = receiptNo,
            paymentId = paymentId,
            studentFeeRecordId = studentFeeRecordId,
            studentId = studentId,
            studentName = studentName,
            amountPaid = amountPaid,
            balanceAfter = balanceAfter,
            paymentMethod = paymentMethod,
            className = className,
            termName = termName,
            academicYearName = academicYearName,
            createdAt = System.currentTimeMillis(),
            receiptUrl = "/api/receipts/$receiptId/print"
        )
    }

    fun findById(
        tenantSchema: String,
        id: Int
    ): ReceiptResponse? = transaction {

        setTenantSchema(tenantSchema)

        ReceiptsTable
            .selectAll()
            .where { ReceiptsTable.id eq id }
            .singleOrNull()
            ?.let { row ->
                ReceiptResponse(
                    id = row[ReceiptsTable.id].value,
                    receiptNo = row[ReceiptsTable.receiptNo],
                    paymentId = row[ReceiptsTable.payment].value,
                    studentFeeRecordId = row[ReceiptsTable.studentFeeRecord].value,
                    studentId = row[ReceiptsTable.student].value,
                    studentName = row[ReceiptsTable.studentName],
                    amountPaid = row[ReceiptsTable.amountPaid],
                    balanceAfter = row[ReceiptsTable.balanceAfter],
                    paymentMethod = row[ReceiptsTable.paymentMethod],
                    className = row[ReceiptsTable.className],
                    termName = row[ReceiptsTable.termName],
                    academicYearName = row[ReceiptsTable.academicYearName],
                    createdAt = row[ReceiptsTable.createdAt],
                    receiptUrl = "/api/receipts/${row[ReceiptsTable.id].value}/print"
                )
            }
    }
}