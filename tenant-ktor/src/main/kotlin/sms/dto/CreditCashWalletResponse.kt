package sms.dto



import kotlinx.serialization.Serializable

@Serializable
data class CreditCashWalletResponse(
    val success: Boolean,
    val message: String,
    val tenantCode: String,
    val cashBalance: String,
    val smsBalance: Int
)