package com.example.billing.routes



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
                            row[TenantsTable.tenantCode].equals(
                                tenantCode,
                                ignoreCase = true
                            )

                val slugMatches =
                    !tenantSlug.isNullOrBlank() &&
                            row[TenantsTable.tenantSlug].equals(
                                tenantSlug,
                                ignoreCase = true
                            )

                codeMatches || slugMatches
            }
            ?.get(TenantsTable.tenantSchema)
    }
}

fun ApplicationCall.resolveSchoolNameOrDefault(
    defaultName: String = "Phena School"
): String {
    /**
     * Optional direct header from frontend.
     * If you want, Vue can send:
     * X-School-Name: schoolNameFromLocalStorage
     */
    val schoolNameHeader = request.header("X-School-Name")

    if (!schoolNameHeader.isNullOrBlank()) {
        return schoolNameHeader
    }

    val tenantCode = request.header("X-Tenant-Code")
    val tenantSlug = request.header("X-Tenant-Slug")
    val tenantSchema = request.header("X-Tenant-Schema")

    return transaction {
        setTenantSchema("public")

        val row = TenantsTable
            .selectAll()
            .firstOrNull { tenantRow ->
                val codeMatches =
                    !tenantCode.isNullOrBlank() &&
                            tenantRow[TenantsTable.tenantCode].equals(
                                tenantCode,
                                ignoreCase = true
                            )

                val slugMatches =
                    !tenantSlug.isNullOrBlank() &&
                            tenantRow[TenantsTable.tenantSlug].equals(
                                tenantSlug,
                                ignoreCase = true
                            )

                val schemaMatches =
                    !tenantSchema.isNullOrBlank() &&
                            tenantRow[TenantsTable.tenantSchema].equals(
                                tenantSchema,
                                ignoreCase = true
                            )

                codeMatches || slugMatches || schemaMatches
            }

        /**
         * Change TenantsTable.schoolName below to your actual school name column.
         *
         * Common alternatives:
         * TenantsTable.name
         * TenantsTable.tenantName
         * TenantsTable.schoolName
         */
        row?.get(TenantsTable.schoolName) ?: defaultName
    }
}