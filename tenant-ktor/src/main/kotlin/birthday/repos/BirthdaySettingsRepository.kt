package birthday.repos




import birthday.dto.BirthdaySettingsResponse
import birthday.tables.BirthdaySettingsTable
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.update

object BirthdaySettingsRepository {

    fun get():
            BirthdaySettingsResponse? {

        return BirthdaySettingsTable
            .selectAll()
            .limit(1)
            .singleOrNull()
            ?.let {

                BirthdaySettingsResponse(
                    enabled =
                        it[
                            BirthdaySettingsTable.enabled
                        ],

                    birthdayMessage =
                        it[
                            BirthdaySettingsTable.birthdayMessage
                        ]
                )
            }
    }

    fun save(
        enabled: Boolean,
        birthdayMessage: String
    ) {

        val existing =
            BirthdaySettingsTable
                .selectAll()
                .limit(1)
                .singleOrNull()

        if (existing == null) {

            BirthdaySettingsTable.insert {

                it[
                    BirthdaySettingsTable.enabled
                ] = enabled

                it[
                    BirthdaySettingsTable.birthdayMessage
                ] = birthdayMessage
            }

        } else {

            BirthdaySettingsTable.update {

                it[
                    BirthdaySettingsTable.enabled
                ] = enabled

                it[
                    BirthdaySettingsTable.birthdayMessage
                ] = birthdayMessage
            }
        }
    }
}