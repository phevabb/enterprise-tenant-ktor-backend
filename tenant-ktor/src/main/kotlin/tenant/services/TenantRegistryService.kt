package com.example.tenant.services



import com.example.tenant.TenantContext
import com.example.tenant.tables.TenantFeaturesTable
import com.example.tenant.tables.TenantsTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.text.get

object TenantRegistryService {

    fun resolveByHost(host: String): TenantContext? {
        val normalizedHost = normalizeHost(host)

        return transaction {
            val tenantRow = TenantsTable
                .selectAll()
                .where {
                    (TenantsTable.defaultDomain eq normalizedHost) or
                            (TenantsTable.primaryDomain eq normalizedHost)
                }
                .limit(1)
                .singleOrNull()
                ?: return@transaction null

            val tenantId = tenantRow[TenantsTable.id]

            val features = TenantFeaturesTable
                .selectAll()
                .where {
                    (TenantFeaturesTable.tenantId eq tenantId) and
                            (TenantFeaturesTable.isEnabled eq true)
                }
                .map { it[TenantFeaturesTable.featureCode] }
                .toSet()

            TenantContext(
                tenantId = tenantId,
                schoolName = tenantRow[TenantsTable.schoolName],
                tenantCode = tenantRow[TenantsTable.tenantCode],
                tenantSlug = tenantRow[TenantsTable.tenantSlug],
                tenantSchema = tenantRow[TenantsTable.tenantSchema],
                defaultDomain = tenantRow[TenantsTable.defaultDomain],
                status = tenantRow[TenantsTable.status],
                features = features
            )
        }
    }

    fun resolveByTenantCode(code: String): TenantContext? {
        val normalizedCode = code.trim().lowercase()

        return transaction {
            val tenantRow = TenantsTable
                .selectAll()
                .where { TenantsTable.tenantCode eq normalizedCode }
                .limit(1)
                .singleOrNull()
                ?: return@transaction null

            val tenantId = tenantRow[TenantsTable.id]

            val features = TenantFeaturesTable
                .selectAll()
                .where {
                    (TenantFeaturesTable.tenantId eq tenantId) and
                            (TenantFeaturesTable.isEnabled eq true)
                }
                .map { it[TenantFeaturesTable.featureCode] }
                .toSet()

            TenantContext(
                tenantId = tenantId,
                schoolName = tenantRow[TenantsTable.schoolName],
                tenantCode = tenantRow[TenantsTable.tenantCode],
                tenantSlug = tenantRow[TenantsTable.tenantSlug],
                tenantSchema = tenantRow[TenantsTable.tenantSchema],
                defaultDomain = tenantRow[TenantsTable.defaultDomain],
                status = tenantRow[TenantsTable.status],
                features = features
            )
        }
    }

    private fun normalizeHost(host: String): String {
        return host
            .trim()
            .lowercase()
            .removePrefix("http://")
            .removePrefix("https://")
            .substringBefore(":")
            .substringBefore("/")
    }

    fun resolveByTenantSlug(slug: String): TenantContext? {
        val normalizedSlug = slug.trim().lowercase()

        return transaction {
            val tenantRow = TenantsTable
                .selectAll()
                .where { TenantsTable.tenantSlug eq normalizedSlug }
                .limit(1)
                .singleOrNull()
                ?: return@transaction null

            val tenantId = tenantRow[TenantsTable.id]

            val features = TenantFeaturesTable
                .selectAll()
                .where {
                    (TenantFeaturesTable.tenantId eq tenantId) and
                            (TenantFeaturesTable.isEnabled eq true)
                }
                .map { it[TenantFeaturesTable.featureCode] }
                .toSet()

            TenantContext(
                tenantId = tenantId,
                schoolName = tenantRow[TenantsTable.schoolName],
                tenantCode = tenantRow[TenantsTable.tenantCode],
                tenantSlug = tenantRow[TenantsTable.tenantSlug],
                tenantSchema = tenantRow[TenantsTable.tenantSchema],
                defaultDomain = tenantRow[TenantsTable.defaultDomain],
                status = tenantRow[TenantsTable.status],
                features = features
            )
        }
    }


    fun findTenantSchemaByTenantCode(tenantCode: String): String? {
        val normalizedCode = tenantCode.trim().lowercase()

        return transaction {
            TenantsTable
                .selectAll()
                .where { TenantsTable.tenantCode eq normalizedCode }
                .limit(1)
                .singleOrNull()
                ?.get(TenantsTable.tenantSchema)
        }
    }
}