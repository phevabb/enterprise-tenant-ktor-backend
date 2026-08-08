package tenant.services



import com.example.tenant.TenantResolver
import com.example.tenant.tables.TenantsTable
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.transactions.transaction

object UpdateSchoolBrandingService {

    fun updateSchoolBranding(
        tenantCode: String,
        schoolName: String,
        schoolLogoUrl: String?,
        schoolMotto: String?,
        location: String?
    ) {

        val normalizedTenantCode = tenantCode
            .trim()
            .lowercase()
            .replace(
                Regex("[^a-z0-9_]"),
                ""
            )

        val tenant = TenantResolver()
            .resolveByTenantCode(
                normalizedTenantCode
            )
            ?: error(
                "Tenant not found."
            )

        transaction {

            TenantsTable.update(
                {
                    TenantsTable.id eq tenant.tenantId
                }
            ) {

                it[TenantsTable.schoolName] =
                    schoolName

                it[TenantsTable.schoolLogoUrl] =
                    schoolLogoUrl

                it[TenantsTable.schoolMotto] =
                    schoolMotto

                it[TenantsTable.location] =
                    location
            }
        }
    }
}