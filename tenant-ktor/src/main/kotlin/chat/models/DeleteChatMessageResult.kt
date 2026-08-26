package chat.models



import kotlinx.serialization.Serializable

@Serializable
data class DeleteChatMessageResult(
    val messageId: Int,
    val conversationId: Int,
    val deletedAt: String,
    val deletedByAccountId: Int,
    val parentAccountId: Int,
    val teacherAccountId: Int
)