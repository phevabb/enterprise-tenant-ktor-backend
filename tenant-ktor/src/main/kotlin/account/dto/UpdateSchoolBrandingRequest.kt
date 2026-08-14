package account.dto


import kotlinx.serialization.Serializable

@Serializable
data class UpdateSchoolBrandingRequest(
    val tenantCode: String,
    val schoolName: String,
    val schoolLogoUrl: String? = null,
    val schoolMotto: String?= null,
    val location: String?= null,
)