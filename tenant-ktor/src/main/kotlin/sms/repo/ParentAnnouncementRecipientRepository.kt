package sms.repo



import com.example.academics.repos.setTenantSchema
import com.example.student.StudentsTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object ParentAnnouncementRecipientRepository {

    fun findParentPhoneNumbers(
        tenantSchema: String,
        audienceType: String,
        classIds: List<Int>
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
            normalizedAudienceType in setOf(
                "all_parents",
                "specific_classes"
            )
        ) {
            "Invalid audience type."
        }

        if (
            normalizedAudienceType ==
            "specific_classes"
        ) {
            require(
                classIds.isNotEmpty()
            ) {
                "Select at least one class."
            }
        }

        val selectedClassIds =
            classIds
                .filter { classId ->
                    classId > 0
                }
                .distinct()
                .toSet()

        return transaction {

            setTenantSchema(
                tenantSchema
            )

            StudentsTable
                .selectAll()
                .where {
                    StudentsTable.isGraduated eq false
                }
                .filter { row ->

                    if (
                        normalizedAudienceType ==
                        "all_parents"
                    ) {
                        true
                    } else {

                        val studentClassId =
                            row[
                                StudentsTable.currentNewGradeClass
                            ]?.value

                        studentClassId != null &&
                                selectedClassIds.contains(
                                    studentClassId
                                )
                    }
                }
                .flatMap { row ->

                    listOfNotNull(
                        row[
                            StudentsTable.contactOfFather
                        ],

                        row[
                            StudentsTable.contactOfMother
                        ]
                    )
                }
                .mapNotNull { phoneNumber ->

                    normalizeGhanaPhoneNumber(
                        phoneNumber
                    )
                }
                .distinct()
        }
    }

    private fun normalizeGhanaPhoneNumber(
        phoneNumber: String?
    ): String? {

        if (phoneNumber.isNullOrBlank()) {
            return null
        }

        val cleaned =
            phoneNumber
                .trim()
                .replace(
                    Regex("[^0-9+]"),
                    ""
                )

        val normalized =
            when {

                cleaned.startsWith("+233") &&
                        cleaned.length == 13 -> {

                    cleaned
                }

                cleaned.startsWith("233") &&
                        cleaned.length == 12 -> {

                    "+$cleaned"
                }

                cleaned.startsWith("0") &&
                        cleaned.length == 10 -> {

                    "+233${cleaned.drop(1)}"
                }

                cleaned.length == 9 -> {

                    "+233$cleaned"
                }

                else -> {
                    return null
                }
            }

        return normalized
    }
}