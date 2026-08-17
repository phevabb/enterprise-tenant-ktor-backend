package superadmin.dto.response



import kotlinx.serialization.Serializable

@Serializable
data class SenderIdResponse(
    val id: Int,
    val tenantCode: String,
    val schoolName: String,
    val senderId: String,
    val status: String,
    val rejectionReason: String?,
    val requestedAt: String,
    val approvedAt: String?,
    val createdAt: String,
    val updatedAt: String?
)