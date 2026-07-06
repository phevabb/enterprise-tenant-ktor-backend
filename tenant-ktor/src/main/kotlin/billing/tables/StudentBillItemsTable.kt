package com.example.billing.tables



import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption

object StudentBillItemsTable : IntIdTable("student_bill_items") {

    val studentBill = reference("student_bill_id", StudentBillsTable,  onDelete = ReferenceOption.CASCADE)

    val itemName = varchar("item_name", 150)

    val description = text("description").nullable()

    val amountCedis = decimal("amount_cedis", 12, 2)

    /**
     * fixed, arrears, discount, optional
     */
    val itemType = varchar("item_type", 50).default("fixed")

    val sortOrder = integer("sort_order").default(0)
}