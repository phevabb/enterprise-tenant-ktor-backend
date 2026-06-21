//package com.example.superadmin.service
//
//
//
//import com.example.tenant.tables.TenantsTable
//import org.jetbrains.exposed.sql.SortOrder
//import org.jetbrains.exposed.sql.selectAll
//import org.jetbrains.exposed.sql.transactions.transaction
//import org.jetbrains.exposed.sql.update
//
//object SuperAdminTenantService {
//
//    fun getAllTenants(): List<AdminTenantResponse> = transaction {
//        TenantsTable
//            .selectAll()
//            .orderBy(TenantsTable.id, SortOrder.DESC)
//            .map {
//                AdminTenantResponse(
//                    id = it[TenantsTable.id].value,
//                    schoolName = it[TenantsTable.schoolName],
//                    tenantCode = it[TenantsTable.tenantCode],
//                    tenantSchema = it[TenantsTable.tenantSchema],
//                    tenantSlug = it[TenantsTable.tenantSlug],
//                    defaultDomain = it[TenantsTable.defaultDomain],
//                    schoolType = it[TenantsTable.schoolType],
//                    location = it[TenantsTable.location],
//                    contactEmail = it[TenantsTable.contactEmail],
//                    accountOwnerName = it[TenantsTable.accountOwnerName],
//                    primaryDomain = it[TenantsTable.primaryDomain],
//                    academicYear = it[TenantsTable.academicYear],
//                    status = it[TenantsTable.status],
//                    createdAt = it[TenantsTable.createdAt]
//                )
//            }
//    }
//
//    fun updateTenantStatus(tenantId: Int, status: String): Boolean = transaction {
//        val updatedRows = TenantsTable.update({ TenantsTable.id eq tenantId }) {
//            it[TenantsTable.status] = status
//        }
//
//        updatedRows > 0
//    }
//}