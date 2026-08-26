package chat.models

import kotlinx.serialization.Serializable

@Serializable
data class DeleteChatMessageResponse(
    val success: Boolean,
    val message: String,
    val messageId: Int,
    val conversationId: Int,
    val deletedAt: String
)