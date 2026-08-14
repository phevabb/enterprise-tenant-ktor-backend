package tenant.dto.response


import kotlinx.serialization.Serializable

@Serializable
data class SchoolLogoUploadResponse(
    val schoolLogoUrl: String,
    val schoolLogoPublicId: String,
    val originalFileName: String?,
    val contentType: String?,
    val sizeBytes: Int
)