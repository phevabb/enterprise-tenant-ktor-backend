package chat.models



import kotlinx.serialization.Serializable

@Serializable
data class CreateParentTeacherConversationRequest(
    val studentId: Int
)


