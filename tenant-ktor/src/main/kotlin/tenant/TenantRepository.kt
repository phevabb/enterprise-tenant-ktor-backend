package tenant

import com.example.tenant.tables.TenantsTable
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun getSchoolLogoPublicId(
    tenantSchema: String
): String? {

    return transaction {

        TenantsTable
            .selectAll()
            .where {
                TenantsTable.tenantSchema eq tenantSchema
            }
            .singleOrNull()
            ?.get(TenantsTable.schoolLogoPublicId)
    }
}