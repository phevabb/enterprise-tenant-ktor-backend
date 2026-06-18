package com.example.tenant


import com.example.academics.repos.setTenantSchema
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.TransactionManager

fun <T> tenantTransaction(tenantSchema: String, block: Transaction.() -> T): T {
    return transaction {
        val safeSchema = tenantSchema.trim().lowercase()

        // IMPORTANT:
        // SET LOCAL applies only to the current transaction
        setTenantSchema(safeSchema)

        block()
    }
}
