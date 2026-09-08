package birthday.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object BirthdaySettingsTable :
    IntIdTable("birthday_settings") {

    val enabled =
        bool("enabled")
            .default(false)

    val birthdayMessage =
        text("birthday_message")
            .default(
                """
                Happy Birthday {student_name}!

                From all of us at {school_name},
                we wish you joy, good health and success.
                """.trimIndent()
            )
}