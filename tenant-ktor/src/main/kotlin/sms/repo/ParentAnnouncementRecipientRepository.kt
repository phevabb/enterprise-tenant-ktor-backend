package sms.repo



import com.example.academics.repos.setTenantSchema
import com.example.student.StudentsTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object ParentAnnouncementRecipientRepository {

    fun findParentPhoneNumbers(
        tenantSchema: String,
        audienceType: String,
        classIds: List<Int>,
        studentIds: List<Int>
    ): List<String> {

        require(tenantSchema.isNotBlank()) {
            "Tenant schema is required."
        }

        val normalizedAudienceType =
            audienceType
                .trim()
                .lowercase()

        require(
            normalizedAudienceType in setOf(
                "all_parents",
                "specific_classes",
                "specific_students"
            )
        ) {
            "Audience type must be all_parents, specific_classes, or specific_students."
        }

        val selectedClassIds =
            classIds
                .filter { classId ->
                    classId > 0
                }
                .distinct()

        val selectedStudentIds =
            studentIds
                .filter { studentId ->
                    studentId > 0
                }
                .distinct()

        when (normalizedAudienceType) {
            "specific_classes" -> {
                require(selectedClassIds.isNotEmpty()) {
                    "Select at least one class."
                }
            }

            "specific_students" -> {
                require(selectedStudentIds.isNotEmpty()) {
                    "Select at least one student."
                }
            }
        }

        return transaction {

            setTenantSchema(
                tenantSchema
            )

            StudentsTable
                .select(
                    StudentsTable.id,
                    StudentsTable.contactOfFather
                )
                .where {
                    when (normalizedAudienceType) {
                        "all_parents" -> {
                            StudentsTable.isGraduated eq false
                        }

                        "specific_classes" -> {
                            (StudentsTable.isGraduated eq false) and
                                    (
                                            StudentsTable.currentNewGradeClass inList
                                                    selectedClassIds
                                            )
                        }

                        "specific_students" -> {
                            (StudentsTable.isGraduated eq false) and
                                    (
                                            StudentsTable.id inList
                                                    selectedStudentIds
                                            )
                        }

                        else -> {
                            error(
                                "Unsupported audience type: $normalizedAudienceType"
                            )
                        }
                    }
                }
                .mapNotNull { row ->

                    val fatherContact =
                        row[
                            StudentsTable.contactOfFather
                        ]

                    normalizeGhanaPhoneNumber(
                        fatherContact
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