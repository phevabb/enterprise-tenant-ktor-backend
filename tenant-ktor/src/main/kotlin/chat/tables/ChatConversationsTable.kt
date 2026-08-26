package chat.tables




import com.example.account.AccountTable
import com.example.student.StudentsTable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime

object ChatConversationsTable : IntIdTable(
    "chat_conversations"
) {

    val student =
        reference(
            "student_id",
            StudentsTable
        )

    val parentAccount =
        reference(
            "parent_account_id",
            AccountTable
        )

    val teacherAccount =
        reference(
            "teacher_account_id",
            AccountTable
        )

    val createdAt =
        datetime("created_at")

    val updatedAt =
        datetime("updated_at")

    val lastMessageAt =
        datetime("last_message_at")
            .nullable()

    val isClosed =
        bool("is_closed")
            .default(false)

    init {
        uniqueIndex(
            student,
            parentAccount,
            teacherAccount
        )
    }
}