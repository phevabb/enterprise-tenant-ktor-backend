package sms.dto

import kotlinx.serialization.Serializable

@Serializable
data class SmsAnnouncementResponse(
    val id: Int,
    val tenantCode: String,
    val schoolName: String,
    val senderId: String,
    val audienceType: String,
    val audienceLabel: String,
    val selectedClassIds: List<Int>,
    val selectedClassNames: List<String>,
    val description: String?,
    val message: String,
    val recipientCount: Int,
    val segmentCount: Int,
    val totalCreditsUsed: Int,
    val smsBalanceBefore: Int,
    val smsBalanceAfter: Int,
    val status: String,
    val providerCampaignId: String?,
    val failureReason: String?,
    val createdAt: String,
    val sentAt: String?
)