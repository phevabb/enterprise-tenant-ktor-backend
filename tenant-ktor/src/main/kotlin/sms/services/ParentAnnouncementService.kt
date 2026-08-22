package sms.services



import com.example.notifications.SmsService
import sms.dto.SendParentAnnouncementRequest
import sms.dto.SendParentAnnouncementResponse
import sms.repo.ParentAnnouncementRecipientRepository


object ParentAnnouncementService {

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
            normalizedAudienceType in setOf(
                "all_parents",
                "specific_classes"
            )
        ) {
            "Audience type must be all_parents or specific_classes."
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

        if (
            normalizedAudienceType ==
            "specific_classes"
        ) {
            require(
                request.classIds.isNotEmpty()
            ) {
                "Select at least one class."
            }
        }

        val normalizedClassIds =
            request.classIds
                .filter { classId ->
                    classId > 0
                }
                .distinct()

        val recipients =
            ParentAnnouncementRecipientRepository
                .findParentPhoneNumbers(
                    tenantSchema = tenantSchema,
                    audienceType = normalizedAudienceType,
                    classIds = normalizedClassIds
                )

        require(
            recipients.isNotEmpty()
        ) {
            "No valid parent phone numbers were found for the selected audience."
        }

        println(
            "[ParentAnnouncementService] " +
                    "tenantCode=$normalizedTenantCode, " +
                    "audienceType=$normalizedAudienceType, " +
                    "classIds=$normalizedClassIds, " +
                    "recipients=${recipients.size}"
        )

        val smsResult =
            SmsService.sendAndCharge(
                tenantCode = normalizedTenantCode,
                recipients = recipients,
                message = normalizedMessage
            )

        return SendParentAnnouncementResponse(
            success = smsResult.success,
            message = smsResult.message,
            senderId = smsResult.senderId,
            audienceType = normalizedAudienceType,
            recipientCount = smsResult.recipientCount,
            segmentCount = smsResult.segmentCount,
            totalCreditsUsed = smsResult.totalSmsUsed,
            smsBalanceBefore = smsResult.smsBalanceBefore,
            smsBalanceAfter = smsResult.smsBalanceAfter
        )
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