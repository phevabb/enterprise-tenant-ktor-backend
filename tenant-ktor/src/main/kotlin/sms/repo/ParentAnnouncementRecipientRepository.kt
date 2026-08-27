package sms.repo

import com.example.student.repos.setTenantSchema
import com.example.student.StudentsTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import sms.util.normalizeGhanaPhoneNumber

class ParentAnnouncementRecipientRepository {

    private val allowedParentAudienceTypes =
        setOf(
            "all_parents",
            "specific_classes",
            "specific_students"
        )

    fun findParentPhoneNumbers(
        tenantSchema: String,
        audienceType: String,
        classIds: List<Int> = emptyList(),
        studentIds: List<Int> = emptyList()
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
                    allowedParentAudienceTypes
        ) {
            "Parent audience type must be all_parents, " +
                    "specific_classes, or specific_students."
        }

        val normalizedClassIds =
            classIds
                .filter { classId ->
                    classId > 0
                }
                .distinct()

        val normalizedStudentIds =
            studentIds
                .filter { studentId ->
                    studentId > 0
                }
                .distinct()

        validateAudienceSelection(
            audienceType =
                normalizedAudienceType,
            classIds =
                normalizedClassIds,
            studentIds =
                normalizedStudentIds
        )

        println(
            "[ParentAnnouncementRecipientRepository] " +
                    "Resolving parent recipients, " +
                    "tenantSchema=$tenantSchema, " +
                    "audienceType=$normalizedAudienceType, " +
                    "classIds=$normalizedClassIds, " +
                    "studentIds=$normalizedStudentIds"
        )

        val recipientPhoneNumbers =
            transaction {
                setTenantSchema(
                    tenantSchema
                )

                val query =
                    StudentsTable
                        .select(
                            StudentsTable.id,
                            StudentsTable.contactOfFather
                        )

                val filteredQuery =
                    when (
                        normalizedAudienceType
                    ) {
                        "all_parents" -> {
                            query.where {
                                StudentsTable.isGraduated eq
                                        false
                            }
                        }

                        "specific_classes" -> {
                            query.where {
                                (
                                        StudentsTable.isGraduated eq
                                                false
                                        ) and
                                        (
                                                StudentsTable
                                                    .currentNewGradeClass inList
                                                        normalizedClassIds
                                                )
                            }
                        }

                        "specific_students" -> {
                            query.where {
                                (
                                        StudentsTable.isGraduated eq
                                                false
                                        ) and
                                        (
                                                StudentsTable.id inList
                                                        normalizedStudentIds
                                                )
                            }
                        }

                        else -> {
                            error(
                                "Unsupported parent audience type: " +
                                        normalizedAudienceType
                            )
                        }
                    }

                filteredQuery
                    .mapNotNull { row ->
                        val parentPhoneNumber =
                            row[
                                StudentsTable.contactOfFather
                            ]

                        normalizeGhanaPhoneNumber(
                            parentPhoneNumber
                        )
                    }
                    .distinct()
            }

        println(
            "[ParentAnnouncementRecipientRepository] " +
                    "Parent recipients resolved, " +
                    "audienceType=$normalizedAudienceType, " +
                    "recipientCount=${recipientPhoneNumbers.size}"
        )

        return recipientPhoneNumbers
    }

    private fun validateAudienceSelection(
        audienceType: String,
        classIds: List<Int>,
        studentIds: List<Int>
    ) {
        when (
            audienceType
        ) {
            "specific_classes" -> {
                require(
                    classIds.isNotEmpty()
                ) {
                    "Select at least one class."
                }
            }

            "specific_students" -> {
                require(
                    studentIds.isNotEmpty()
                ) {
                    "Select at least one student."
                }
            }

            "all_parents" -> {
                Unit
            }

            else -> {
                error(
                    "Unsupported parent audience type: " +
                            audienceType
                )
            }
        }
    }
}