package complaints.tables



import com.example.account.AccountTable
import com.example.student.StudentsTable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime

object ParentComplaintsTable : IntIdTable(
    name = "parent_complaints"
) {
    val complaintNumber =
        varchar(
            name = "complaint_number",
            length = 40
        )
            .uniqueIndex()

    val student =
        reference(
            name = "student_id",
            foreign = StudentsTable
        )

    val parentAccount =
        reference(
            name = "parent_account_id",
            foreign = AccountTable
        )

    val category =
        varchar(
            name = "category",
            length = 50
        )

    val subject =
        varchar(
            name = "subject",
            length = 150
        )

    val description =
        text(
            name = "description"
        )

    val priority =
        varchar(
            name = "priority",
            length = 20
        )

    val status =
        varchar(
            name = "status",
            length = 30
        )

    val assignedAdminAccount =
        reference(
            name = "assigned_admin_account_id",
            foreign = AccountTable
        )
            .nullable()

    val createdAt =
        datetime(
            name = "created_at"
        )

    val updatedAt =
        datetime(
            name = "updated_at"
        )

    val lastActivityAt =
        datetime(
            name = "last_activity_at"
        )

    val resolvedAt =
        datetime(
            name = "resolved_at"
        )
            .nullable()

    val closedAt =
        datetime(
            name = "closed_at"
        )
            .nullable()
}
