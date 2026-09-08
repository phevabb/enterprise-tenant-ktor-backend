package tenant.repository

import com.example.tenant.TenantContext
import com.example.tenant.tables.TenantsTable
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

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


fun getAllTenants(): List<TenantContext> {

    return transaction {

        TenantsTable
            .selectAll()
            .map { row ->

                TenantContext(
                    tenantId =
                        row[TenantsTable.id],

                    schoolName =
                        row[TenantsTable.schoolName],

                    tenantCode =
                        row[TenantsTable.tenantCode],

                    tenantSlug =
                        row[TenantsTable.tenantSlug],

                    tenantSchema =
                        row[TenantsTable.tenantSchema],

                    defaultDomain =
                        row[TenantsTable.defaultDomain],

                    status =
                        row[TenantsTable.status],

                    features =
                        emptySet()
                )
            }
    }
}



fun updateSchoolLogoByTenantCode(
    tenantCode: String,
    schoolLogoUrl: String,
    schoolLogoPublicId: String
): Boolean {

    return transaction {

        TenantsTable.update(
            {
                TenantsTable.tenantCode eq tenantCode
            }
        ) {

            it[TenantsTable.schoolLogoUrl] =
                schoolLogoUrl

            it[TenantsTable.schoolLogoPublicId] =
                schoolLogoPublicId
        } > 0
    }
}


fun updateSchoolLogo(
    tenantSchema: String,
    schoolLogoUrl: String,
    schoolLogoPublicId: String
): Boolean {

    return transaction {

        TenantsTable.update(
            { TenantsTable.tenantSchema eq tenantSchema }
        ) {

            it[TenantsTable.schoolLogoUrl] =
                schoolLogoUrl

            it[TenantsTable.schoolLogoPublicId] =
                schoolLogoPublicId
        } > 0
    }
}

fun clearSchoolLogo(
    tenantSchema: String
): Boolean

{

    return transaction {

        TenantsTable.update(
            { TenantsTable.tenantSchema eq tenantSchema }
        ) {

            it[schoolLogoUrl] = null
            it[schoolLogoPublicId] = null
        } > 0
    }
}



















