package com.example.tenant.repository



import com.example.tenant.dto.SuperAdminTenantResponse
import com.example.tenant.dto.response.PublicTenantLoginResponse
import com.example.tenant.tables.TenantsTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

object SuperAdminTenantRepository {

    fun findAllForSuperAdmin(
        search: String?,
        status: String?
    ): List<SuperAdminTenantResponse> = transaction {
        val tenants = TenantsTable
            .selectAll()
            .orderBy(TenantsTable.id, SortOrder.DESC)
            .map { row ->
                SuperAdminTenantResponse(
                    id = row[TenantsTable.id],
                    schoolName = row[TenantsTable.schoolName],
                    tenantCode = row[TenantsTable.tenantCode],
                    tenantSchema = row[TenantsTable.tenantSchema],
                    tenantSlug = row[TenantsTable.tenantSlug],
                    defaultDomain = row[TenantsTable.defaultDomain],

                    schoolType = row[TenantsTable.schoolType],
                    location = row[TenantsTable.location],
                    contactEmail = row[TenantsTable.contactEmail],
                    accountOwnerName = row[TenantsTable.accountOwnerName],
                    primaryDomain = row[TenantsTable.primaryDomain],

                    academicYear = row[TenantsTable.academicYear],
                    status = row[TenantsTable.status],
                    createdAt = row[TenantsTable.createdAt],

                    schemaName = row[TenantsTable.tenantSchema],
                    schemaStatus = if (row[TenantsTable.tenantSchema].isNotBlank()) {
                        "created"
                    } else {
                        "missing"
                    }
                )
            }

        val filteredByStatus = when (status?.trim()?.lowercase()) {
            "active" -> tenants.filter { it.status == "active" }
            "inactive" -> tenants.filter { it.status == "inactive" }
            "suspended" -> tenants.filter { it.status == "suspended" }
            "provisioning" -> tenants.filter { it.status == "provisioning" }
            "failed" -> tenants.filter { it.status == "failed" }
            else -> tenants
        }

        val normalizedSearch = search?.trim()?.lowercase()

        if (normalizedSearch.isNullOrBlank()) {
            filteredByStatus
        } else {
            filteredByStatus.filter { tenant ->
                listOfNotNull(
                    tenant.schoolName,
                    tenant.tenantCode,
                    tenant.tenantSchema,
                    tenant.tenantSlug,
                    tenant.defaultDomain,
                    tenant.schoolType,
                    tenant.location,
                    tenant.contactEmail,
                    tenant.accountOwnerName,
                    tenant.primaryDomain,
                    tenant.academicYear,
                    tenant.status,
                    tenant.createdAt
                )
                    .joinToString(" ")
                    .lowercase()
                    .contains(normalizedSearch)
            }
        }
    }

    fun updateStatusByTenantCode(
        tenantCode: String,
        status: String
    ): Boolean = transaction {
        val normalizedTenantCode = normalizeTenantCode(tenantCode)
        val normalizedStatus = status.trim().lowercase()

        val updatedRows = TenantsTable.update({
            TenantsTable.tenantCode eq normalizedTenantCode
        }) {
            it[TenantsTable.status] = normalizedStatus
        }

        updatedRows > 0
    }


    fun findPublicTenantBySlug(
        tenantSlug: String
    ): PublicTenantLoginResponse? = transaction {
        val normalizedSlug = tenantSlug
            .trim()
            .lowercase()
            .replace(Regex("[^a-z0-9]"), "")

        if (normalizedSlug.isBlank()) {
            return@transaction null
        }

        TenantsTable
            .selectAll()
            .where { TenantsTable.tenantSlug eq normalizedSlug }
            .limit(1)
            .singleOrNull()
            ?.let { row ->
                PublicTenantLoginResponse(
                    schoolName = row[TenantsTable.schoolName],
                    tenantCode = row[TenantsTable.tenantCode],
                    tenantSlug = row[TenantsTable.tenantSlug],
                    status = row[TenantsTable.status]
                )
            }
    }


    private fun normalizeTenantCode(value: String): String {
        return value
            .trim()
            .lowercase()
            .replace(Regex("[^a-z0-9_]"), "")
    }
}