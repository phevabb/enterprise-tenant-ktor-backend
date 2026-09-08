package birthday.tables

import com.example.student.StudentsTable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime

object BirthdayMessageLogTable :
    IntIdTable("birthday_message_log") {

    val student =
        reference(
            "student_id",
            StudentsTable
        )

    val year =
        integer("year")

    val sentAt =
        datetime("sent_at")

    init {
        uniqueIndex(
            student,
            year
        )
    }
}