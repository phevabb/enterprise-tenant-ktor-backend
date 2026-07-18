package com.example.superadmin.repo

import com.example.academics.repos.setTenantSchema
import com.example.student.StudentsTable
import com.example.tenant.dto.response.InternalTenantBillingSummaryItem
import com.example.tenant.tables.TenantsTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction


object SuperAdminBillingTenantRepository {

    suspend fun findAllForSuperAdminBilling(
        search: String? = null,
        status: String? = null
    ): List<InternalTenantBillingSummaryItem> {
        return newSuspendedTransaction(Dispatchers.IO) {
            val tenants = TenantsTable
                .selectAll()
                .orderBy(TenantsTable.schoolName to SortOrder.ASC)
                .map { row ->
                    TenantBillingSchemaRaw(
                        tenantId = row[TenantsTable.id],
                        schoolName = row[TenantsTable.schoolName],
                        tenantCode = row[TenantsTable.tenantCode],
                        tenantSchema = row[TenantsTable.tenantSchema],
                        location = row[TenantsTable.location],
                        academicYear = row[TenantsTable.academicYear],
                        status = row[TenantsTable.status]
                    )
                }
                .filter { tenant ->
                    val matchesSearch = if (search.isNullOrBlank()) {
                        true
                    } else {
                        val cleanSearch = search.trim().lowercase()

                        tenant.schoolName.lowercase().contains(cleanSearch) ||
                                tenant.tenantCode.lowercase().contains(cleanSearch) ||
                                tenant.location.orEmpty().lowercase().contains(cleanSearch)
                    }

                    val matchesStatus = if (status.isNullOrBlank()) {
                        true
                    } else {
                        tenant.status.equals(status.trim(), ignoreCase = true)
                    }

                    matchesSearch && matchesStatus
                }

            println("[BILLING SUMMARY] TOTAL TENANTS = ${tenants.size}")


            println("[BILLING SUMMARY] START PROCESSING")
            val overallStart = System.currentTimeMillis()

            tenants.map { tenant ->

                val start = System.currentTimeMillis()

                val studentCount = try {

                    println(
                        "[BILLING SUMMARY] START tenantCode=${tenant.tenantCode}, schema=${tenant.tenantSchema}"
                    )

                    setTenantSchema(tenant.tenantSchema)

                    println(
                        "[BILLING SUMMARY] SCHEMA SET tenantCode=${tenant.tenantCode}"
                    )

                    val count = StudentsTable
                        .selectAll()
                        .count()
                        .toInt()

                    println(
                        "[BILLING SUMMARY] END tenantCode=${tenant.tenantCode}, count=$count, took=${
                            System.currentTimeMillis() - start
                        }ms"
                    )

                    count

                } catch (e: Exception) {

                    println(
                        "[BILLING SUMMARY] FAILED tenantCode=${tenant.tenantCode}, schema=${tenant.tenantSchema}, error=${e.message}"
                    )

                    0
                }

                InternalTenantBillingSummaryItem(
                    tenantId = tenant.tenantId,
                    schoolName = tenant.schoolName,
                    tenantCode = tenant.tenantCode,
                    location = tenant.location,
                    academicYear = tenant.academicYear,
                    status = tenant.status,
                    studentCount = studentCount
                )
            }.also {
                println(
                    "[BILLING SUMMARY] FINISHED ALL TENANTS IN ${
                        System.currentTimeMillis() - overallStart
                    } ms"
                )
            }
        }
    }

    private data class TenantBillingSchemaRaw(
        val tenantId: Int,
        val schoolName: String,
        val tenantCode: String,
        val tenantSchema: String,
        val location: String?,
        val academicYear: String?,
        val status: String
    )
}