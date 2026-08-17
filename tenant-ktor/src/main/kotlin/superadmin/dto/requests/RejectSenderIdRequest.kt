package superadmin.dto.requests



import kotlinx.serialization.Serializable

@Serializable
data class RejectSenderIdRequest(
    val rejectionReason: String
)