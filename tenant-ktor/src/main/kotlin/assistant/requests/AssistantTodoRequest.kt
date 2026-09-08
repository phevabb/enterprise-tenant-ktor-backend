package assistant.requests

import kotlinx.serialization.Serializable

@Serializable
data class AssistantTodoRequest(
    val title: String,
    val description: String? = null,
    val dueDate: String? = null,
)