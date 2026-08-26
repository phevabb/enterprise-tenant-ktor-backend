package chat.tables


import com.example.account.AccountTable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.datetime

object ChatMessagesTable : IntIdTable(
    "chat_messages"
) {

    val deletedAt =
        datetime("deleted_at")
            .nullable()

    val deletedByAccount =
        reference(
            name = "deleted_by_account_id",
            foreign = AccountTable
        )
            .nullable()


    val conversation =
        reference(
            name = "conversation_id",
            foreign = ChatConversationsTable,
            onDelete = ReferenceOption.CASCADE
        )

    val senderAccount =
        reference(
            "sender_account_id",
            AccountTable
        )

    val content =
        text("content")

    val createdAt =
        datetime("created_at")

    val readAt =
        datetime("read_at")
            .nullable()
}
