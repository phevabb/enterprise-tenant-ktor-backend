package sms.repo


import com.example.academics.repos.setTenantSchema
import com.example.student.tables.NewGradeClassTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import sms.dto.SendParentAnnouncementRequest
import sms.dto.SendParentAnnouncementResponse
import sms.dto.SmsAnnouncementResponse
import sms.tables.SmsAnnouncementsTable
import java.time.Instant
import org.jetbrains.exposed.sql.insert

object SmsAnnouncementRepository {


    private fun buildAudienceLabel(
        audienceType: String,
        request: SendParentAnnouncementRequest,
        recipientCount: Int
    ): String {
        return when (audienceType) {
            "all_parents" -> {
                "All Parents"
            }

            "specific_classes" -> {
                val classCount =
                    request.classIds
                        .filter { classId ->
                            classId > 0
                        }
                        .distinct()
                        .size

                if (classCount == 1) {
                    "1 Selected Class"
                } else {
                    "$classCount Selected Classes"
                }
            }

            "specific_students" -> {
                val studentCount =
                    request.studentIds
                        .filter { studentId ->
                            studentId > 0
                        }
                        .distinct()
                        .size

                if (studentCount == 1) {
                    "1 Selected Student"
                } else {
                    "$studentCount Selected Students"
                }
            }

            "all_staff" -> {
                "All Staff"
            }

            "specific_staff" -> {
                val staffCount =
                    request.staffIds
                        .filter { staffId ->
                            staffId > 0
                        }
                        .distinct()
                        .size

                if (staffCount == 1) {
                    "1 Selected Staff Member"
                } else {
                    "$staffCount Selected Staff Members"
                }
            }

            "custom_numbers" -> {
                if (recipientCount == 1) {
                    "1 Custom Number"
                } else {
                    "$recipientCount Custom Numbers"
                }
            }

            else -> {
                "SMS Recipients"
            }
        }
    }

    fun saveAnnouncement(
        schoolName: String,
        request: SendParentAnnouncementRequest,
        response: SendParentAnnouncementResponse
    ): Int {
        val now =
            Instant.now().toString()

        val normalizedTenantCode =
            normalizeTenantCode(
                request.tenantCode
            )

        val normalizedAudienceType =
            request.audienceType
                .trim()
                .lowercase()

        val audienceLabel =
            buildAudienceLabel(
                audienceType =
                    normalizedAudienceType,
                request =
                    request,
                recipientCount =
                    response.recipientCount
            )

        val selectedClassIds =
            request.classIds
                .filter { classId ->
                    classId > 0
                }
                .distinct()
                .takeIf { classIds ->
                    normalizedAudienceType ==
                            "specific_classes" &&
                            classIds.isNotEmpty()
                }
                ?.joinToString(
                    separator = ","
                )

        return transaction {
            setTenantSchema(
                "public"
            )

            val insertedId =
                SmsAnnouncementsTable.insert {
                    it[tenantCode] =
                        normalizedTenantCode

                    it[SmsAnnouncementsTable.schoolName] =
                        schoolName

                    it[senderId] =
                        response.senderId
                            ?.trim()
                            ?.takeIf { value ->
                                value.isNotBlank()
                            }
                            ?: "Unknown"

                    it[SmsAnnouncementsTable.audienceType] =
                        normalizedAudienceType

                    it[SmsAnnouncementsTable.audienceLabel] =
                        audienceLabel

                    it[SmsAnnouncementsTable.selectedClassIds] =
                        selectedClassIds

                    it[description] =
                        request.description
                            ?.trim()
                            ?.takeIf { value ->
                                value.isNotBlank()
                            }

                    it[message] =
                        request.message.trim()

                    it[recipientCount] =
                        response.recipientCount

                    it[segmentCount] =
                        response.segmentCount

                    it[totalCreditsUsed] =
                        response.totalCreditsUsed

                    it[smsBalanceBefore] =
                        response.smsBalanceBefore

                    it[smsBalanceAfter] =
                        response.smsBalanceAfter

                    it[status] =
                        if (response.success) {
                            "sent"
                        } else {
                            "failed"
                        }

                    it[providerCampaignId] =
                        null

                    it[providerResponse] =
                        null

                    it[failureReason] =
                        if (response.success) {
                            null
                        } else {
                            response.message.take(
                                500
                            )
                        }

                    it[createdAt] =
                        now

                    it[sentAt] =
                        if (response.success) {
                            now
                        } else {
                            null
                        }

                    it[updatedAt] =
                        now
                } get SmsAnnouncementsTable.id

            println(
                "[SmsAnnouncementRepository] " +
                        "announcementId=${insertedId.value}, " +
                        "audienceType=$normalizedAudienceType, " +
                        "audienceLabel=$audienceLabel"
            )

            insertedId.value
        }
    }

    fun findByTenantCode(
        tenantCode: String,
        tenantSchema: String
    ): List<SmsAnnouncementResponse> {

        val normalizedTenantCode =
            normalizeTenantCode(
                tenantCode
            )

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

        val classNamesById =
            transaction {

                setTenantSchema(
                    tenantSchema
                )

                NewGradeClassTable
                    .selectAll()
                    .associate { row ->

                        val classId =
                            row[
                                NewGradeClassTable.id
                            ].value

                        val className =
                            row[
                                NewGradeClassTable.name
                            ]

                        classId to className
                    }
            }

        return transaction {

            setTenantSchema(
                "public"
            )

            SmsAnnouncementsTable
                .selectAll()
                .where {
                    SmsAnnouncementsTable.tenantCode eq
                            normalizedTenantCode
                }
                .orderBy(
                    SmsAnnouncementsTable.id,
                    SortOrder.DESC
                )
                .map { row ->

                    row.toResponse(
                        classNamesById =
                            classNamesById
                    )
                }
        }
    }

    private fun ResultRow.toResponse(
        classNamesById: Map<Int, String>
    ): SmsAnnouncementResponse {

        val storedClassIds =
            this[
                SmsAnnouncementsTable.selectedClassIds
            ]

        val selectedClassIds =
            storedClassIds
                ?.split(",")
                ?.mapNotNull { value ->

                    value
                        .trim()
                        .toIntOrNull()
                }
                ?.distinct()
                ?: emptyList()

        val selectedClassNames =
            selectedClassIds
                .mapNotNull { classId ->

                    classNamesById[
                        classId
                    ]
                }

        val audienceType =
            this[
                SmsAnnouncementsTable.audienceType
            ]

        val storedAudienceLabel =
            this[
                SmsAnnouncementsTable.audienceLabel
            ]

        val resolvedAudienceLabel =
            when {

                audienceType.equals(
                    "all_parents",
                    ignoreCase = true
                ) -> {

                    "All Parents"
                }

                selectedClassNames.isNotEmpty() -> {

                    selectedClassNames.joinToString(
                        separator = ", "
                    )
                }

                storedAudienceLabel.isNotBlank() -> {

                    storedAudienceLabel
                }

                else -> {

                    "Selected Classes"
                }
            }

        return SmsAnnouncementResponse(
            id =
                this[
                    SmsAnnouncementsTable.id
                ].value,

            tenantCode =
                this[
                    SmsAnnouncementsTable.tenantCode
                ],

            schoolName =
                this[
                    SmsAnnouncementsTable.schoolName
                ],

            senderId =
                this[
                    SmsAnnouncementsTable.senderId
                ],

            audienceType =
                audienceType,

            audienceLabel =
                resolvedAudienceLabel,

            selectedClassIds =
                selectedClassIds,

            selectedClassNames =
                selectedClassNames,

            description =
                this[
                    SmsAnnouncementsTable.description
                ],

            message =
                this[
                    SmsAnnouncementsTable.message
                ],

            recipientCount =
                this[
                    SmsAnnouncementsTable.recipientCount
                ],

            segmentCount =
                this[
                    SmsAnnouncementsTable.segmentCount
                ],

            totalCreditsUsed =
                this[
                    SmsAnnouncementsTable.totalCreditsUsed
                ],

            smsBalanceBefore =
                this[
                    SmsAnnouncementsTable.smsBalanceBefore
                ],

            smsBalanceAfter =
                this[
                    SmsAnnouncementsTable.smsBalanceAfter
                ],

            status =
                this[
                    SmsAnnouncementsTable.status
                ],

            providerCampaignId =
                this[
                    SmsAnnouncementsTable.providerCampaignId
                ],

            failureReason =
                this[
                    SmsAnnouncementsTable.failureReason
                ],

            createdAt =
                this[
                    SmsAnnouncementsTable.createdAt
                ],

            sentAt =
                this[
                    SmsAnnouncementsTable.sentAt
                ]
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
    }}