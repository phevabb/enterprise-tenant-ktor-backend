package com.example.billing.repos



import com.example.academics.repos.setTenantSchema
import com.example.tenant.tables.TenantsTable
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.header
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun ApplicationCall.resolveTenantSchemaOrNull(): String? {
    val tenantSchemaHeader = request.header("X-Tenant-Schema")

    if (!tenantSchemaHeader.isNullOrBlank()) {
        return tenantSchemaHeader
    }

    val tenantCode = request.header("X-Tenant-Code")
    val tenantSlug = request.header("X-Tenant-Slug")

    if (tenantCode.isNullOrBlank() && tenantSlug.isNullOrBlank()) {
        return null
    }

    return transaction {
        setTenantSchema("public")

        TenantsTable
            .selectAll()
            .firstOrNull { row ->
                val codeMatches =
                    !tenantCode.isNullOrBlank() &&
                            row[TenantsTable.tenantCode].equals(tenantCode, ignoreCase = true)

                val slugMatches =
                    !tenantSlug.isNullOrBlank() &&
                            row[TenantsTable.tenantSlug].equals(tenantSlug, ignoreCase = true)

                codeMatches || slugMatches
            }
            ?.get(TenantsTable.tenantSchema)
    }
}