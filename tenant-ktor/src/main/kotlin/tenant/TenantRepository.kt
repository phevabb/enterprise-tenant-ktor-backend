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



//fun findDefaultDomainBySchema(
//    tenantSchema: String
//): String {
//
//    val normalizedTenantCode =
//        tenantSchema
//            .removePrefix("tenant_")
//            .trim()
//
//    return transaction {
//        TenantsTable
//            .select(
//                TenantsTable.defaultDomain
//            )
//            .where {
//                TenantsTable.tenantCode eq
//                        normalizedTenantCode
//            }
//            .limit(1)
//            .singleOrNull()
//            ?.get(
//                TenantsTable.defaultDomain
//            )
//            ?: throw IllegalArgumentException(
//                "The school's default domain was not found."
//            )
//    }
//}