package com.example.tenant.tables

import org.jetbrains.exposed.sql.Table

object TenantFeaturesTable : Table("tenant_features") {
    val id = integer("id").autoIncrement()
    val tenantId = integer("tenant_id").references(TenantsTable.id)
    val featureCode = varchar("feature_code", 100)
    val isEnabled = bool("is_enabled").default(true)

    override val primaryKey = PrimaryKey(id)
}