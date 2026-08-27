package sms.repo

import sms.util.normalizeGhanaPhoneNumber

object AnnouncementRecipientRepository {

    private val parentRecipientRepository =
        ParentAnnouncementRecipientRepository()

    private val staffRecipientRepository =
        StaffAnnouncementRecipientRepository()

    private val allowedAudienceTypes =
        setOf(
            "all_parents",
            "specific_classes",
            "specific_students",
            "all_staff",
            "specific_staff",
            "custom_numbers"
        )

    fun findRecipientPhoneNumbers(
        tenantSchema: String,
        audienceType: String,
        classIds: List<Int> = emptyList(),
        studentIds: List<Int> = emptyList(),
        staffIds: List<Int> = emptyList(),
        customNumbers: List<String> = emptyList()
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
                    allowedAudienceTypes
        ) {
            "Unsupported SMS audience type: " +
                    normalizedAudienceType
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

        val normalizedStaffIds =
            staffIds
                .filter { staffId ->
                    staffId > 0
                }
                .distinct()

        validateRequiredSelections(
            audienceType =
                normalizedAudienceType,
            classIds =
                normalizedClassIds,
            studentIds =
                normalizedStudentIds,
            staffIds =
                normalizedStaffIds,
            customNumbers =
                customNumbers
        )

        val recipientPhoneNumbers =
            when (
                normalizedAudienceType
            ) {
                "all_parents",
                "specific_classes",
                "specific_students" -> {
                    parentRecipientRepository
                        .findParentPhoneNumbers(
                            tenantSchema =
                                tenantSchema,
                            audienceType =
                                normalizedAudienceType,
                            classIds =
                                normalizedClassIds,
                            studentIds =
                                normalizedStudentIds
                        )
                }

                "all_staff",
                "specific_staff" -> {
                    staffRecipientRepository
                        .findStaffPhoneNumbers(
                            tenantSchema =
                                tenantSchema,
                            audienceType =
                                normalizedAudienceType,
                            staffIds =
                                normalizedStaffIds
                        )
                }

                "custom_numbers" -> {
                    normalizeCustomPhoneNumbers(
                        customNumbers =
                            customNumbers
                    )
                }

                else -> {
                    error(
                        "Unsupported SMS audience type: " +
                                normalizedAudienceType
                    )
                }
            }

        val uniquePhoneNumbers =
            recipientPhoneNumbers
                .mapNotNull { phoneNumber ->
                    normalizeGhanaPhoneNumber(
                        phoneNumber
                    )
                }
                .distinct()

        require(
            uniquePhoneNumbers.isNotEmpty()
        ) {
            getEmptyRecipientMessage(
                audienceType =
                    normalizedAudienceType
            )
        }

        println(
            "[AnnouncementRecipientRepository] " +
                    "audienceType=$normalizedAudienceType, " +
                    "recipientCount=${uniquePhoneNumbers.size}"
        )

        return uniquePhoneNumbers
    }

    private fun validateRequiredSelections(
        audienceType: String,
        classIds: List<Int>,
        studentIds: List<Int>,
        staffIds: List<Int>,
        customNumbers: List<String>
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

            "specific_staff" -> {
                require(
                    staffIds.isNotEmpty()
                ) {
                    "Select at least one staff member."
                }
            }

            "custom_numbers" -> {
                require(
                    customNumbers.any { phoneNumber ->
                        phoneNumber.isNotBlank()
                    }
                ) {
                    "Enter at least one recipient phone number."
                }
            }

            "all_parents",
            "all_staff" -> {
                Unit
            }

            else -> {
                error(
                    "Unsupported SMS audience type: " +
                            audienceType
                )
            }
        }
    }

    private fun normalizeCustomPhoneNumbers(
        customNumbers: List<String>
    ): List<String> {
        val suppliedNumbers =
            customNumbers
                .flatMap { suppliedValue ->
                    suppliedValue.split(
                        ',',
                        ';',
                        '\n',
                        '\r'
                    )
                }
                .map { phoneNumber ->
                    phoneNumber.trim()
                }
                .filter { phoneNumber ->
                    phoneNumber.isNotBlank()
                }
                .distinct()

        require(
            suppliedNumbers.isNotEmpty()
        ) {
            "Enter at least one recipient phone number."
        }

        val invalidPhoneNumbers =
            suppliedNumbers
                .filter { phoneNumber ->
                    normalizeGhanaPhoneNumber(
                        phoneNumber
                    ) == null
                }

        require(
            invalidPhoneNumbers.isEmpty()
        ) {
            "The following phone numbers are invalid: " +
                    invalidPhoneNumbers.joinToString(
                        separator =
                            ", "
                    )
        }

        val normalizedPhoneNumbers =
            suppliedNumbers
                .mapNotNull { phoneNumber ->
                    normalizeGhanaPhoneNumber(
                        phoneNumber
                    )
                }
                .distinct()

        require(
            normalizedPhoneNumbers.isNotEmpty()
        ) {
            "No valid recipient phone numbers were provided."
        }

        return normalizedPhoneNumbers
    }

    private fun getEmptyRecipientMessage(
        audienceType: String
    ): String {
        return when (
            audienceType
        ) {
            "all_parents" -> {
                "No valid parent phone numbers were found."
            }

            "specific_classes" -> {
                "No valid parent phone numbers were found " +
                        "for the selected classes."
            }

            "specific_students" -> {
                "No valid parent phone numbers were found " +
                        "for the selected students."
            }

            "all_staff" -> {
                "No valid staff phone numbers were found."
            }

            "specific_staff" -> {
                "No valid phone numbers were found for " +
                        "the selected staff members."
            }

            "custom_numbers" -> {
                "No valid custom phone numbers were provided."
            }

            else -> {
                "No valid SMS recipients were found."
            }
        }
    }
}