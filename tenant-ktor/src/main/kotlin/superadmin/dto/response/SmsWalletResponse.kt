package superadmin.dto.response



import kotlinx.serialization.Serializable

@Serializable
data class SmsWalletResponse(
    val id: Int,
    val tenantCode: String,
    val schoolName: String,
    val cashBalance: String,
    val smsBalance: Int,
    val totalCashLoaded: String,
    val totalSmsPurchased: Int,
    val totalSmsUsed: Int,
    val status: String,
    val createdAt: String,
    val updatedAt: String?
)