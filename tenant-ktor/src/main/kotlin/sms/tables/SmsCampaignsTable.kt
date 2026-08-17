package sms.tables


import org.jetbrains.exposed.sql.Table

object SmsCampaignsTable : Table("sms_campaigns") {

    val id = integer("id").autoIncrement()

    val tenantCode = varchar("tenant_code", 100)

    val senderId = varchar("sender_id", 11)

    val message = text("message")

    val recipientCount = integer("recipient_count")

    val segmentCount = integer("segment_count")

    val totalCreditsUsed = integer("total_credits_used")

    val status = varchar("status", 30).default("pending")
    /*
     * pending
     * sent
     * failed
     * partial
     */

    val providerCampaignId = varchar("provider_campaign_id", 150).nullable()

    val createdAt = varchar("created_at", 50)

    override val primaryKey =
        PrimaryKey(id)
}