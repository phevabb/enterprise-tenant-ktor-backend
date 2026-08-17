package superadmin.dto.response



import kotlinx.serialization.Serializable

@Serializable
data class SenderIdMessageResponse(
    val success: Boolean,
    val message: String
)