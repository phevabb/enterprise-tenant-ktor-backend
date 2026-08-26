package chat.models



import kotlinx.serialization.Serializable

@Serializable
data class ChatSocketEvent(
    val type: String,

    val conversationId: Int? = null,

    val messageId: Int? = null,

    val senderAccountId: Int? = null,

    val senderName: String? = null,

    val content: String? = null,

    val sentAt: String? = null,

    val readAt: String? = null,

    val errorMessage: String? = null,


    val deletedAt: String? = null,
    val deletedByAccountId: Int? = null

)
