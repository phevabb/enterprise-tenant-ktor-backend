package assistant.responses



import kotlinx.serialization.Serializable

@Serializable
data class AssistantNoteResponse(
    val id: Int,
    val title: String,
    val content: String,
    val createdAt: String,
    val updatedAt: String
)