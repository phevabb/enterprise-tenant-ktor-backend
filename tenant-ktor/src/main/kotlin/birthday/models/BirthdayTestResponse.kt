package birthday.models



import kotlinx.serialization.Serializable

@Serializable
data class BirthdayTestResponse(
    val success: Boolean,
    val schoolName: String,
    val tenantSchema: String,
    val message: String
)