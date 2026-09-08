package assistant.requests

import kotlinx.serialization.Serializable

@Serializable
data class AssistantNoteRequest(
    val title: String,
    val content: String
)