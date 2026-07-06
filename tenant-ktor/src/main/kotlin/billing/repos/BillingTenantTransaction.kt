package com.example.billing.repos



import com.example.academics.repos.setTenantSchema
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction

fun <T> tenantTransaction(
    tenantSchema: String,
    block: Transaction.() -> T
): T {
    return transaction {
        setTenantSchema(tenantSchema)
        block()
    }
}