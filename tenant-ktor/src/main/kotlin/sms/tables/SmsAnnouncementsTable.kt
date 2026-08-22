package sms.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object SmsAnnouncementsTable : IntIdTable(
    name = "sms_announcements"
) {

    val tenantCode =
        varchar(
            name = "tenant_code",
            length = 100
        )

    val schoolName =
        varchar(
            name = "school_name",
            length = 255
        )

    val senderId =
        varchar(
            name = "sender_id",
            length = 11
        )

    val audienceType =
        varchar(
            name = "audience_type",
            length = 40
        )

    val audienceLabel =
        varchar(
            name = "audience_label",
            length = 500
        )

    val selectedClassIds =
        text(
            name = "selected_class_ids"
        ).nullable()

    val description =
        varchar(
            name = "description",
            length = 500
        ).nullable()

    val message =
        text(
            name = "message"
        )

    val recipientCount =
        integer(
            name = "recipient_count"
        )

    val segmentCount =
        integer(
            name = "segment_count"
        )

    val totalCreditsUsed =
        integer(
            name = "total_credits_used"
        )

    val smsBalanceBefore =
        integer(
            name = "sms_balance_before"
        )

    val smsBalanceAfter =
        integer(
            name = "sms_balance_after"
        )

    val status =
        varchar(
            name = "status",
            length = 30
        ).default("pending")

    val providerCampaignId =
        varchar(
            name = "provider_campaign_id",
            length = 150
        ).nullable()

    val providerResponse =
        text(
            name = "provider_response"
        ).nullable()

    val failureReason =
        varchar(
            name = "failure_reason",
            length = 500
        ).nullable()

    val createdAt =
        varchar(
            name = "created_at",
            length = 50
        )

    val sentAt =
        varchar(
            name = "sent_at",
            length = 50
        ).nullable()

    val updatedAt =
        varchar(
            name = "updated_at",
            length = 50
        ).nullable()

    init {

        index(
            customIndexName =
                "idx_sms_announcements_tenant_code",
            isUnique = false,
            tenantCode
        )

        index(
            customIndexName =
                "idx_sms_announcements_status",
            isUnique = false,
            status
        )

        index(
            customIndexName =
                "idx_sms_announcements_created_at",
            isUnique = false,
            createdAt
        )
    }
}
