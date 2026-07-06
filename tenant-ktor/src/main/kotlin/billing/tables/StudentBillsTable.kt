package com.example.billing.tables



import com.example.student.StudentsTable
import com.example.student.tables.AcademicYearTable
import com.example.student.tables.TermTable

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import java.math.BigDecimal

object StudentBillsTable : IntIdTable("student_bills") {

    val student = reference("student_id", StudentsTable, onDelete = ReferenceOption.CASCADE)

    val billTemplate = reference("bill_template_id", BillTemplatesTable)

    val academicYear = reference("academic_year_id", AcademicYearTable)

    val academicTerm = reference("academic_term_id", TermTable)

    /**
     * Example:
     * BILL-2025-000001
     */
    val billNumber = varchar("bill_number", 100).uniqueIndex()

    /**
     * Class should come from the student record at generation time.
     * If you want to print the class on the bill later, store it as text snapshot.
     */
    val classNameSnapshot = varchar("class_name_snapshot", 150).nullable()

    val subTotalCedis = decimal("sub_total_cedis", 12, 2).default(BigDecimal.ZERO)

    val arrearsCedis = decimal("arrears_cedis", 12, 2).default(BigDecimal.ZERO)

    val discountCedis = decimal("discount_cedis", 12, 2).default(BigDecimal.ZERO)

    val totalAmountCedis = decimal("total_amount_cedis", 12, 2).default(BigDecimal.ZERO)

    val amountPaidCedis = decimal("amount_paid_cedis", 12, 2).default(BigDecimal.ZERO)

    val balanceCedis = decimal("balance_cedis", 12, 2).default(BigDecimal.ZERO)

    /**
     * pending, partial, paid, cancelled
     */
    val status = varchar("status", 50).default("pending")

    val dueDateEpochMillis = long("due_date_epoch_millis").nullable()

    val createdAtEpochMillis = long("created_at_epoch_millis")

    val updatedAtEpochMillis = long("updated_at_epoch_millis").nullable()
}