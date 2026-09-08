package birthday.dto



import kotlinx.serialization.Serializable

@Serializable
data class BirthdaySettingsResponse(
    val enabled: Boolean,
    val birthdayMessage: String
)