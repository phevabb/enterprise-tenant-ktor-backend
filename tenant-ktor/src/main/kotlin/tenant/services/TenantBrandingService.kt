package tenant.services


import com.example.academics.repos.setTenantSchema
import com.example.tenant.tables.TenantsTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

object TenantBrandingService {

    fun updateSchoolLogoForTenant(
        tenantCode: String,
        schoolLogoUrl: String
    ): Boolean {

        val normalizedTenantCode =
            normalizeTenantCode(
                tenantCode
            )

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

            if (tenantRow == null) {
                return@transaction false
            }

            val tenantId =
                tenantRow[TenantsTable.id]

            val updatedRows =
                TenantsTable.update(
                    {
                        TenantsTable.id eq tenantId
                    }
                ) {
                    it[TenantsTable.schoolLogoUrl] =
                        schoolLogoUrl
                }

            updatedRows > 0
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