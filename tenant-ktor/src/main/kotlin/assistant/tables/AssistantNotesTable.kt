package assistant.tables



import org.jetbrains.exposed.dao.id.IntIdTable

object AssistantNotesTable :
    IntIdTable("assistant_notes") {

    val title =
        varchar("title", 255)

    val content =
        text("content")

    val createdAt =
        varchar("created_at", 50)

    val updatedAt =
        varchar("updated_at", 50)
}