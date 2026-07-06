package com.example.billing.tables


import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption

object BillTemplateItemsTable : IntIdTable("bill_template_items") {

    val billTemplate = reference("bill_template_id", BillTemplatesTable, onDelete = ReferenceOption.CASCADE)

    val itemName = varchar("item_name", 150)

    val description = text("description").nullable()

    /**
     * Nullable because some items may be placeholders,
     * optional fields, or calculated later.
     */
    val amountCedis = decimal("amount_cedis", 12, 2).nullable()

    /**
     * fixed      = normal item like Tuition, Utility
     * arrears    = calculated later from student records
     * discount   = discount item
     * optional   = optional fee item
     */
    val itemType = varchar("item_type", 50).default("fixed")

    val sortOrder = integer("sort_order").default(0)

    val isActive = bool("is_active").default(true)
}