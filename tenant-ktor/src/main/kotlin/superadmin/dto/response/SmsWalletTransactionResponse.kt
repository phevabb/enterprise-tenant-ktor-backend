package superadmin.dto.response



import kotlinx.serialization.Serializable

@Serializable
data class SmsWalletTransactionResponse(
    val id: Int,
    val tenantCode: String,
    val type: String,
    val amountCash: String?,
    val amountSms: Int?,
    val cashBalanceBefore: String,
    val cashBalanceAfter: String,
    val smsBalanceBefore: Int,
    val smsBalanceAfter: Int,
    val description: String,
    val reference: String?,
    val createdAt: String
)