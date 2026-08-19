package tenant.routes

import kotlinx.serialization.Serializable

@Serializable
data class UpdateSchoolBrandingWithoutLogoRequest(
    val tenantCode: String,
    val schoolName: String,
    val schoolMotto: String?,
    val location: String?
)