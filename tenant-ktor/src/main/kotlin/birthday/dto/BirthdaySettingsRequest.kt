package birthday.dto



import kotlinx.serialization.Serializable

@Serializable
data class BirthdaySettingsRequest(
    val enabled: Boolean,
    val birthdayMessage: String
)