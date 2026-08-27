package complaints.tables



import com.example.account.AccountTable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime

object ComplaintRepliesTable : IntIdTable(
    name = "complaint_replies"
) {
    val complaint =
        reference(
            name = "complaint_id",
            foreign = ParentComplaintsTable
        )

    val senderAccount =
        reference(
            name = "sender_account_id",
            foreign = AccountTable
        )

    val senderRole =
        varchar(
            name = "sender_role",
            length = 20
        )

    val content =
        text(
            name = "content"
        )

    val isInternal =
        bool(
            name = "is_internal"
        )
            .default(false)

    val createdAt =
        datetime(
            name = "created_at"
        )

    val readAt =
        datetime(
            name = "read_at"
        )
            .nullable()
}