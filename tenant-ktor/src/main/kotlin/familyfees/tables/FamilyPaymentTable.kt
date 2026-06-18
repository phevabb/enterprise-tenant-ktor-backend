package com.example.familyfees.tables

import com.example.fees.tables.StudentFeeRecordTable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption

object FamilyPaymentTable: IntIdTable() {
    val family_fee_record = reference("family_fee_record", FamilyFeeRecordTable, onDelete = ReferenceOption.CASCADE)
    val date_created = long("date_created")
    val amount = integer("amount")
    val payment_method = varchar("payment_method", 255).nullable()


}