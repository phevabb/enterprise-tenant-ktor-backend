package sms.dto



import kotlinx.serialization.Serializable

@Serializable
data class PurchaseSmsCreditsResponse(
    val success: Boolean,
    val message: String,
    val tenantCode: String = "",
    val amountSpent: String = "0.00",
    val smsCreditsPurchased: Int = 0,
    val cashBalance: String = "0.00",
    val smsBalance: Int = 0,
    val totalSmsPurchased: Int = 0
)