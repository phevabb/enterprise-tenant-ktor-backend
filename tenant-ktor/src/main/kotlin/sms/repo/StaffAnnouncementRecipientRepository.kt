package sms.repo

import com.example.staff.tables.StaffTable
import com.example.student.repos.setTenantSchema
import org.jetbrains.exposed.sql.transactions.transaction
import sms.util.normalizeGhanaPhoneNumber

class StaffAnnouncementRecipientRepository {

    fun findStaffPhoneNumbers(
        tenantSchema: String,
        audienceType: String,
        staffIds: List<Int>
    ): List<String> {
        require(
            tenantSchema.isNotBlank()
        ) {
            "Tenant schema is required."
        }

        val normalizedAudienceType =
            audienceType
                .trim()
                .lowercase()

        require(
            normalizedAudienceType in
                    ALLOWED_STAFF_AUDIENCE_TYPES
        ) {
            "Staff audience type must be all_staff " +
                    "or specific_staff."
        }

        val selectedStaffIds =
            staffIds
                .filter { staffId ->
                    staffId > 0
                }
                .distinct()

        if (
            normalizedAudienceType ==
            "specific_staff"
        ) {
            require(
                selectedStaffIds.isNotEmpty()
            ) {
                "Select at least one staff member."
            }
        }

        return transaction {
            setTenantSchema(
                tenantSchema
            )

            val query =
                StaffTable
                    .select(
                        StaffTable.id,
                        StaffTable.tel
                    )

            val filteredQuery =
                when (
                    normalizedAudienceType
                ) {
                    "all_staff" -> {
                        query
                    }

                    "specific_staff" -> {
                        query.where {
                            StaffTable.id inList
                                    selectedStaffIds
                        }
                    }

                    else -> {
                        error(
                            "Unsupported staff audience type: " +
                                    normalizedAudienceType
                        )
                    }
                }

            filteredQuery
                .mapNotNull { row ->
                    val phoneNumber =
                        row[
                            StaffTable.tel
                        ]

                    normalizeGhanaPhoneNumber(
                        phoneNumber
                    )
                }
                .distinct()
        }
    }

    companion object {
        private val ALLOWED_STAFF_AUDIENCE_TYPES =
            setOf(
                "all_staff",
                "specific_staff"
            )
    }
}
