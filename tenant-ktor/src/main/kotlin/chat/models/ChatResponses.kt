package chat.models



import kotlinx.serialization.Serializable

@Serializable
data class ChatConversationResponse(
    val id: Int,
    val studentId: Int,
    val studentName: String,
    val classId: Int,
    val className: String,
    val parentAccountId: Int,
    val parentName: String,
    val teacherAccountId: Int,
    val teacherName: String,
    val teacherOnline: Boolean = false,
    val parentOnline: Boolean = false,
    val lastMessage: String? = null,
    val lastMessageAt: String? = null,
    val unreadCount: Int = 0,
    val createdAt: String,
)


@Serializable
data class ChatMessageResponse(
    val id: Int,
    val conversationId: Int,
    val senderAccountId: Int,
    val senderName: String,
    val content: String,
    val createdAt: String,
    val readAt: String? = null,
    val isMine: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: String? = null,
    val deletedByAccountId: Int? = null
)



@Serializable
data class StartConversationRequest(
    val studentId: Int
)

@Serializable
data class SendChatMessageRequest(
    val content: String
)