package sms.tables


import org.jetbrains.exposed.sql.Table

object SmsSenderIdsTable : Table("sms_sender_ids") {

    val id = integer("id").autoIncrement()

    val tenantCode = varchar("tenant_code", 100)

    val schoolName = varchar("school_name", 255)

    val senderId = varchar("sender_id", 11)

    val status = varchar("status", 30).default("pending")
    /*
     * pending
     * approved
     * rejected
     * suspended
     */

    val rejectionReason = varchar("rejection_reason", 500).nullable()

    val requestedAt = varchar("requested_at", 50)

    val approvedAt = varchar("approved_at", 50).nullable()

    val createdAt = varchar("created_at", 50)

    val updatedAt = varchar("updated_at", 50).nullable()

    override val primaryKey =
        PrimaryKey(id)
}