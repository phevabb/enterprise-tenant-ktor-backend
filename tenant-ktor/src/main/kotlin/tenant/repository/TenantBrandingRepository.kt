package tenant.repository



import com.example.academics.repos.setTenantSchema
import com.example.tenant.tables.TenantsTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

object TenantBrandingRepository {

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

        require(
            normalizedTenantCode.isNotBlank()
        ) {
            "Tenant code is required."
        }

        require(
            schoolName.isNotBlank()
        ) {
            "School name is required."
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

            if (tenantRow == null) {

                println(
                    "[TenantBrandingRepository] Tenant not found. " +
                            "incomingTenantCode=$tenantCode, " +
                            "normalizedTenantCode=$normalizedTenantCode"
                )

                return@transaction false
            }

            val tenantId =
                tenantRow[TenantsTable.id]

            val existingLogoUrl =
                tenantRow[TenantsTable.schoolLogoUrl]

            val existingLogoPublicId =
                tenantRow[TenantsTable.schoolLogoPublicId]

            println(
                "[TenantBrandingRepository] Tenant found. " +
                        "tenantId=$tenantId, " +
                        "existingLogoUrl=$existingLogoUrl, " +
                        "existingLogoPublicId=$existingLogoPublicId"
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

                    /*
                     * Do not update these fields here:
                     *
                     * TenantsTable.schoolLogoUrl
                     * TenantsTable.schoolLogoPublicId
                     *
                     * This preserves the existing Cloudinary logo.
                     */
                }

            println(
                "[TenantBrandingRepository] Updated rows=$updatedRows, " +
                        "tenantCode=$tenantCode, " +
                        "preservedLogoUrl=$existingLogoUrl"
            )

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