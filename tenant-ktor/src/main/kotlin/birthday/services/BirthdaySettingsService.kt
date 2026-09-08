package birthday.services


import birthday.dto.BirthdaySettingsRequest
import birthday.repos.BirthdaySettingsRepository
import com.example.tenant.tenantTransaction

object BirthdaySettingsService {

    fun get(
        tenantSchema: String
    ) = tenantTransaction(
        tenantSchema
    ) {

        BirthdaySettingsRepository.get()
    }

    fun save(
        tenantSchema: String,
        request: BirthdaySettingsRequest
    ) = tenantTransaction(
        tenantSchema
    ) {

        BirthdaySettingsRepository.save(
            enabled =
                request.enabled,

            birthdayMessage =
                request.birthdayMessage
        )
    }
}