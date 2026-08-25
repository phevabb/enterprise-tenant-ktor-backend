package tenant

import com.example.tenant.tables.TenantsTable
import org.jetbrains.exposed.sql.transactions.transaction

fun findDefaultDomainBySchema(
    tenantSchema: String
): String {

    require(
        tenantSchema.isNotBlank()
    ) {
        "Tenant schema is required."
    }

    return transaction {

        TenantsTable
            .select(
                TenantsTable.defaultDomain
            )
            .where {
                TenantsTable.tenantSchema eq
                        tenantSchema
            }
            .limit(1)
            .singleOrNull()
            ?.get(
                TenantsTable.defaultDomain
            )
            ?.trim()
            ?.takeIf { domain ->
                domain.isNotBlank()
            }
            ?: throw IllegalArgumentException(
                "The school's default domain was not found for tenant schema '$tenantSchema'."
            )
    }
}