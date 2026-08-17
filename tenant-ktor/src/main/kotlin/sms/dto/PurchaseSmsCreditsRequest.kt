package sms.dto



import kotlinx.serialization.Serializable

@Serializable
data class PurchaseSmsCreditsRequest(
    val tenantCode: String,
    val amount: String,
    val smsCredits: Int
)