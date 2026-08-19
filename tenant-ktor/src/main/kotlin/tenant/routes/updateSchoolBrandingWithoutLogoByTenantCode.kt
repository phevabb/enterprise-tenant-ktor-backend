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

private fun updateSchoolBrandingWithoutLogoByTenantCode(
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
                "[updateSchoolBrandingWithoutLogo] " +
                        "Tenant not found. incoming=$tenantCode, " +
                        "normalized=$normalizedTenantCode"
            )

            return@transaction false
        }

        val tenantId =
            tenantRow[TenantsTable.id]

        val existingLogoUrl =
            tenantRow[TenantsTable.schoolLogoUrl]

        println(
            "[updateSchoolBrandingWithoutLogo] " +
                    "Tenant found. id=$tenantId, " +
                    "existingLogoUrl=$existingLogoUrl"
        )

        val updatedRows =
            TenantsTable.update(
                {
                    TenantsTable.id eq tenantId
                }
            ) {
                it[TenantsTable.schoolName] =
                    schoolName.trim()

                it[TenantsTable.schoolMotto] =
                    schoolMotto
                        ?.trim()
                        ?.takeIf { value ->
                            value.isNotBlank()
                        }

                it[TenantsTable.location] =
                    location
                        ?.trim()
                        ?.takeIf { value ->
                            value.isNotBlank()
                        }

                // Do not update schoolLogoUrl.
                // Do not update schoolLogoPublicId.
            }

        println(
            "[updateSchoolBrandingWithoutLogo] " +
                    "Updated rows=$updatedRows, " +
                    "preservedLogoUrl=$existingLogoUrl"
        )

        updatedRows > 0
    }
}