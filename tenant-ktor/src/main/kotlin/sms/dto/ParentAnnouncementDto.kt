package sms.dto

import kotlinx.serialization.Serializable

@Serializable
data class SendParentAnnouncementRequest(
    val tenantCode: String,
    val audienceType: String,
    val classIds: List<Int> = emptyList(),
    val studentIds: List<Int> = emptyList(),
    val message: String,
    val description: String? = null
)

@Serializable
data class SendParentAnnouncementResponse(
    val success: Boolean,
    val message: String,
    val senderId: String? = null,
    val audienceType: String? = null,
    val recipientCount: Int = 0,
    val segmentCount: Int = 0,
    val totalCreditsUsed: Int = 0,
    val smsBalanceBefore: Int = 0,
    val smsBalanceAfter: Int = 0
)