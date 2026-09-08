package birthday.repos

import birthday.tables.BirthdayMessageLogTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import java.time.LocalDateTime

object BirthdayMessageLogRepository {

    fun alreadySent(
        studentId: Int,
        year: Int
    ): Boolean {

        return BirthdayMessageLogTable
            .selectAll()
            .where {
                (BirthdayMessageLogTable.student eq studentId) and
                        (BirthdayMessageLogTable.year eq year)
            }
            .count() > 0
    }

    fun save(
        studentId: Int,
        year: Int
    ) {

        BirthdayMessageLogTable.insert {

            it[student] = studentId

            it[BirthdayMessageLogTable.year] =
                year

            it[sentAt] =
                LocalDateTime.now()
        }
    }
}