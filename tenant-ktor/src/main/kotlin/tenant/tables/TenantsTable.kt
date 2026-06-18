package com.example.tenant.tables





import org.jetbrains.exposed.sql.Table



object TenantsTable : Table("tenants") {
    val id = integer("id").autoIncrement()
    val schoolName = varchar("school_name", 255)
    val tenantCode = varchar("tenant_code", 100).uniqueIndex()

    // Internal DB schema name
    val tenantSchema = varchar("schema_name", 150).uniqueIndex()

    // Opaque public-facing slug
    val tenantSlug = varchar("tenant_slug", 120).uniqueIndex()

    // Default generated school domain like t-9fk3q8a2.phena.app
    val defaultDomain = varchar("default_domain", 255).uniqueIndex()

    val schoolType = varchar("school_type", 120).nullable()
    val location = varchar("location", 255).nullable()
    val contactEmail = varchar("contact_email", 255).nullable()
    val accountOwnerName = varchar("account_owner_name", 255).nullable()

    // Keep this for future custom domain support if you want
    val primaryDomain = varchar("primary_domain", 255).nullable()

    val academicYear = varchar("academic_year", 30).nullable()
    val status = varchar("status", 30).default("provisioning")
    val createdAt = varchar("created_at", 50)

    override val primaryKey = PrimaryKey(id)
}



