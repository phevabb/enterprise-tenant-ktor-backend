package tenant.dto.response



import kotlinx.serialization.Serializable

@Serializable
data class SchoolBrandingUpdateResponse(
    val success: Boolean,
    val message: String
)