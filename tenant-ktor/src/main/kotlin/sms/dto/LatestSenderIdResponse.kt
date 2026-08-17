package sms.dto


import kotlinx.serialization.Serializable

@Serializable
data class LatestSenderIdResponse(
    val success: Boolean,
    val available: Boolean,
    val message: String,
    val senderId: SenderIdResponse? = null
)