package sms.dto



import kotlinx.serialization.Serializable

@Serializable
data class CreditCashWalletRequest(
    val tenantCode: String,
    val amount: String,
    val reference: String
)