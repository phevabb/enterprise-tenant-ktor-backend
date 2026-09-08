package assistant.tables



import org.jetbrains.exposed.dao.id.IntIdTable

object AssistantTodoTable :
    IntIdTable("assistant_todos") {

    val title =
        varchar("title", 255)

    val description =
        text("description")
            .nullable()

    val completed =
        bool("completed")
            .default(false)

    val dueDate =
        varchar("due_date", 50)
            .nullable()

    val createdAt =
        varchar("created_at", 50)

    val updatedAt =
        varchar("updated_at", 50)
}