package sms.dto


import kotlinx.serialization.Serializable

@Serializable
data class SmsBalanceResponse(
    val tenantCode: String,
    val smsBalance: Int
)