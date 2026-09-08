package assistant.responses

import kotlinx.serialization.Serializable

@Serializable
data class AssistantTodoResponse(
    val id: Int,
    val title: String,
    val description: String?,
    val completed: Boolean,
    val dueDate: String?,
    val createdAt: String,
    val updatedAt: String
)