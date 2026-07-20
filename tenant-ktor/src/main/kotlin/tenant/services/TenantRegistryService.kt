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

    fun resolveByTenantSlug(
        slug: String
    ): TenantContext? {

        val overallStart = System.currentTimeMillis()

        val normalizedSlug = slug.trim().lowercase()

        println(
            "[TENANT RESOLUTION] START slug=$normalizedSlug"
        )

        val result = transaction {

            val transactionStart = System.currentTimeMillis()

            println(
                "[TENANT RESOLUTION] TRANSACTION START slug=$normalizedSlug"
            )

            val tenantQueryStart = System.currentTimeMillis()

            val tenantRow = TenantsTable
                .selectAll()
                .where {
                    TenantsTable.tenantSlug eq normalizedSlug
                }
                .limit(1)
                .singleOrNull()

            println(
                "[TENANT RESOLUTION] TENANT QUERY TOOK ${
                    System.currentTimeMillis() - tenantQueryStart
                } ms"
            )

            if (tenantRow == null) {
                println(
                    "[TENANT RESOLUTION] TENANT NOT FOUND slug=$normalizedSlug"
                )

                return@transaction null
            }

            val tenantId = tenantRow[TenantsTable.id]

            println(
                "[TENANT RESOLUTION] TENANT FOUND id=$tenantId slug=$normalizedSlug"
            )

            val featuresStart = System.currentTimeMillis()

            val features = TenantFeaturesTable
                .selectAll()
                .where {
                    (TenantFeaturesTable.tenantId eq tenantId) and
                            (TenantFeaturesTable.isEnabled eq true)
                }
                .map {
                    it[TenantFeaturesTable.featureCode]
                }
                .toSet()

            println(
                "[TENANT RESOLUTION] FEATURES QUERY TOOK ${
                    System.currentTimeMillis() - featuresStart
                } ms"
            )

            println(
                "[TENANT RESOLUTION] FEATURES COUNT=${features.size}"
            )

            val context = TenantContext(
                tenantId = tenantId,
                schoolName = tenantRow[TenantsTable.schoolName],
                tenantCode = tenantRow[TenantsTable.tenantCode],
                tenantSlug = tenantRow[TenantsTable.tenantSlug],
                tenantSchema = tenantRow[TenantsTable.tenantSchema],
                defaultDomain = tenantRow[TenantsTable.defaultDomain],
                status = tenantRow[TenantsTable.status],
                features = features
            )

            println(
                "[TENANT RESOLUTION] TRANSACTION COMPLETE IN ${
                    System.currentTimeMillis() - transactionStart
                } ms"
            )

            context
        }

        println(
            "[TENANT RESOLUTION] METHOD COMPLETE IN ${
                System.currentTimeMillis() - overallStart
            } ms"
        )

        return result
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