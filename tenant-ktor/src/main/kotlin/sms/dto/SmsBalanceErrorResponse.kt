package sms.dto



import kotlinx.serialization.Serializable

@Serializable
data class SmsBalanceErrorResponse(
    val success: Boolean,
    val message: String
)