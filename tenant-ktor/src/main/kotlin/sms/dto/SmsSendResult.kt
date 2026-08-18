package sms.dto

import kotlinx.serialization.Serializable

@Serializable
data class SmsSendResult(
    val success: Boolean,
    val message: String,
    val senderId: String? = null,
    val recipientCount: Int = 0,
    val segmentCount: Int = 0,
    val totalSmsUsed: Int = 0,
    val smsBalanceBefore: Int = 0,
    val smsBalanceAfter: Int = 0,
    val providerResponse: String? = null
)