package com.example.familyfees.tables




import org.jetbrains.exposed.dao.id.IntIdTable

object FamilyReceiptsTable : IntIdTable("family_receipts") {

    val receiptNo = varchar("receipt_no", 50).uniqueIndex()

    val familyPaymentId = integer("family_payment_id")
    val familyFeeRecordId = integer("family_fee_record_id")

    val familyName = varchar("family_name", 222)

    // store wards as a single string snapshot
    val wardsCsv = text("wards_csv")

    val amountPaid = integer("amount_paid")
    val balanceAfter = integer("balance_after")
    val paymentMethod = varchar("payment_method", 20)

    val termName = varchar("term_name", 50).nullable()
    val academicYearName = varchar("academic_year_name", 50).nullable()

    val createdAt = long("created_at")
}