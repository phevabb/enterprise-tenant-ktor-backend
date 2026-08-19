package tenant.routes

import com.example.student.repos.setTenantSchema
import com.example.tenant.tables.TenantsTable
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update


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

fun updateSchoolBrandingWithoutLogoByTenantCode(
    tenantCode: String,
    schoolName: String,
    schoolMotto: String?,
    location: String?
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

            println(
                "❌ Tenant not found while updating branding without logo. tenantCode=$tenantCode normalized=$normalizedTenantCode"
            )

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

                it[TenantsTable.schoolName] =
                    schoolName

                it[TenantsTable.schoolMotto] =
                    schoolMotto

                it[TenantsTable.location] =
                    location
            }

        println(
            "✅ Branding without logo update rows=$updatedRows tenantCode=$tenantCode schoolName=$schoolName"
        )

        updatedRows > 0
    }
}