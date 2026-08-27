package sms.services

import com.example.notifications.SmsService
import sms.dto.SendParentAnnouncementRequest
import sms.dto.SendParentAnnouncementResponse
import sms.repo.AnnouncementRecipientRepository

object ParentAnnouncementService {

    private val allowedAudienceTypes =
        setOf(
            "all_parents",
            "specific_classes",
            "specific_students",
            "all_staff",
            "specific_staff",
            "custom_numbers"
        )

    suspend fun sendParentAnnouncement(
        tenantSchema: String,
        request: SendParentAnnouncementRequest
    ): SendParentAnnouncementResponse {
        val normalizedTenantCode =
            normalizeTenantCode(
                request.tenantCode
            )

        val normalizedAudienceType =
            request.audienceType
                .trim()
                .lowercase()

        val normalizedMessage =
            request.message
                .trim()

        val normalizedClassIds =
            request.classIds
                .filter { classId ->
                    classId > 0
                }
                .distinct()

        val normalizedStudentIds =
            request.studentIds
                .filter { studentId ->
                    studentId > 0
                }
                .distinct()

        val normalizedStaffIds =
            request.staffIds
                .filter { staffId ->
                    staffId > 0
                }
                .distinct()

        val normalizedCustomNumbers =
            request.customNumbers
                .flatMap { value ->
                    value.split(
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
            normalizedTenantCode.isNotBlank()
        ) {
            "Tenant code is required."
        }

        require(
            tenantSchema.isNotBlank()
        ) {
            "Tenant schema is required."
        }

        require(
            normalizedAudienceType in
                    allowedAudienceTypes
        ) {
            "Audience type must be all_parents, " +
                    "specific_classes, specific_students, " +
                    "all_staff, specific_staff, " +
                    "or custom_numbers."
        }

        require(
            normalizedMessage.isNotBlank()
        ) {
            "Announcement message is required."
        }

        require(
            normalizedMessage.length <= 480
        ) {
            "Announcement message cannot exceed 480 characters."
        }

        validateAudienceSelection(
            audienceType =
                normalizedAudienceType,
            classIds =
                normalizedClassIds,
            studentIds =
                normalizedStudentIds,
            staffIds =
                normalizedStaffIds,
            customNumbers =
                normalizedCustomNumbers
        )

        val recipientPhoneNumbers =
            AnnouncementRecipientRepository
                .findRecipientPhoneNumbers(
                    tenantSchema =
                        tenantSchema,
                    audienceType =
                        normalizedAudienceType,
                    classIds =
                        normalizedClassIds,
                    studentIds =
                        normalizedStudentIds,
                    staffIds =
                        normalizedStaffIds,
                    customNumbers =
                        normalizedCustomNumbers
                )

        require(
            recipientPhoneNumbers.isNotEmpty()
        ) {
            getEmptyRecipientsMessage(
                normalizedAudienceType
            )
        }

        println(
            "[ParentAnnouncementService] " +
                    "tenantCode=$normalizedTenantCode, " +
                    "audienceType=$normalizedAudienceType, " +
                    "classIds=$normalizedClassIds, " +
                    "studentIds=$normalizedStudentIds, " +
                    "staffIds=$normalizedStaffIds, " +
                    "customNumberCount=${normalizedCustomNumbers.size}, " +
                    "recipientCount=${recipientPhoneNumbers.size}"
        )

        val smsResult =
            SmsService.sendAndCharge(
                tenantCode =
                    normalizedTenantCode,
                recipients =
                    recipientPhoneNumbers,
                message =
                    normalizedMessage
            )

        println(
            "[ParentAnnouncementService] " +
                    "sendSuccess=${smsResult.success}, " +
                    "recipientCount=${smsResult.recipientCount}, " +
                    "segmentCount=${smsResult.segmentCount}, " +
                    "creditsUsed=${smsResult.totalSmsUsed}"
        )

        return SendParentAnnouncementResponse(
            success =
                smsResult.success,
            message =
                smsResult.message,
            senderId =
                smsResult.senderId,
            audienceType =
                normalizedAudienceType,
            recipientCount =
                smsResult.recipientCount,
            segmentCount =
                smsResult.segmentCount,
            totalCreditsUsed =
                smsResult.totalSmsUsed,
            smsBalanceBefore =
                smsResult.smsBalanceBefore,
            smsBalanceAfter =
                smsResult.smsBalanceAfter
        )
    }

    private fun validateAudienceSelection(
        audienceType: String,
        classIds: List<Int>,
        studentIds: List<Int>,
        staffIds: List<Int>,
        customNumbers: List<String>
    ) {
        when (audienceType) {
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
                    customNumbers.isNotEmpty()
                ) {
                    "Enter at least one recipient phone number."
                }
            }
        }
    }

    private fun getEmptyRecipientsMessage(
        audienceType: String
    ): String {
        return when (audienceType) {
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

    private fun normalizeTenantCode(
        tenantCode: String
    ): String {
        return tenantCode
            .trim()
            .lowercase()
            .replace(
                Regex("[^a-z0-9_]"),
                ""
            )
    }
}