package sms.services


import com.example.academics.repos.setTenantSchema
import com.example.tenant.tables.TenantsTable
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

data class ResolvedSmsTenant(
    val tenantCode: String,
    val tenantSchema: String,
    val schoolName: String
)

object SmsTenantResolver {

    fun resolveByTenantCode(
        tenantCode: String
    ): ResolvedSmsTenant? {

        val normalizedTenantCode =
            normalizeTenantCode(
                tenantCode
            )

        if (normalizedTenantCode.isBlank()) {
            return null
        }

        return transaction {

            setTenantSchema(
                "public"
            )

            val tenantRow =
                TenantsTable
                    .selectAll()
                    .firstOrNull { row ->

                        normalizeTenantCode(
                            row[TenantsTable.tenantCode]
                        ) == normalizedTenantCode
                    }
                    ?: return@transaction null

            ResolvedSmsTenant(
                tenantCode = normalizeTenantCode(
                    tenantRow[TenantsTable.tenantCode]
                ),
                tenantSchema =
                    tenantRow[TenantsTable.tenantSchema],

                schoolName =
                    tenantRow[TenantsTable.schoolName]
            )
        }
    }

    private fun normalizeTenantCode(
        tenantCode: String
    ): String {

        return tenantCode
            .trim()
            .lowercase()
            .replace(
                Regex("[^a-z0-9_]"),
                ""
            )
    }
}