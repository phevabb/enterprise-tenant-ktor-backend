package complaints.repositories


import complaints.models.AssignComplaintRequest
import complaints.models.ComplaintCategory
import complaints.models.ComplaintPriority
import complaints.models.ComplaintReplyResponse
import complaints.models.ComplaintSenderRole
import complaints.models.ComplaintStatus
import complaints.models.CreateAdminComplaintReplyRequest
import complaints.models.CreateComplaintReplyRequest
import complaints.models.CreateParentComplaintRequest
import complaints.models.ParentComplaintResponse
import complaints.tables.ComplaintRepliesTable
import complaints.tables.ParentComplaintsTable
import com.example.account.AccountTable
import com.example.student.StudentsTable
import com.example.student.repos.setTenantSchema
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

object ComplaintRepository {

    fun createParentComplaint(
        tenantSchema: String,
        authenticatedAccountId: Int,
        request: CreateParentComplaintRequest
    ): ParentComplaintResponse {
        requireTenantAndAccount(
            tenantSchema = tenantSchema,
            accountId = authenticatedAccountId
        )

        require(
            request.studentId > 0
        ) {
            "A valid student ID is required."
        }

        val category =
            request.category
                .trim()
                .uppercase()

        val priority =
            request.priority
                .trim()
                .uppercase()

        val subject =
            request.subject
                .trim()

        val description =
            request.description
                .trim()

        require(
            category in ComplaintCategory.allowedValues
        ) {
            "A valid complaint category is required."
        }

        require(
            priority in ComplaintPriority.allowedValues
        ) {
            "A valid complaint priority is required."
        }

        require(
            subject.isNotBlank()
        ) {
            "Complaint subject is required."
        }

        require(
            subject.length <= 150
        ) {
            "Complaint subject cannot exceed 150 characters."
        }

        require(
            description.isNotBlank()
        ) {
            "Complaint details are required."
        }

        require(
            description.length <= 3000
        ) {
            "Complaint details cannot exceed 3,000 characters."
        }

        return transaction {
            setTenantSchema(
                tenantSchema
            )

            val studentRow =
                StudentsTable
                    .select(
                        StudentsTable.id,
                        StudentsTable.user
                    )
                    .where {
                        StudentsTable.id eq
                                request.studentId
                    }
                    .limit(1)
                    .singleOrNull()
                    ?: throw IllegalArgumentException(
                        "The selected student was not found."
                    )

            val studentAccountId =
                studentRow[
                    StudentsTable.user
                ].value

            require(
                studentAccountId ==
                        authenticatedAccountId
            ) {
                "You can only submit complaints for your own student account."
            }

            val now =
                LocalDateTime.now()

            val complaintNumber =
                generateComplaintNumber()

            val complaintId =
                ParentComplaintsTable
                    .insertAndGetId { statement ->
                        statement[
                            ParentComplaintsTable.complaintNumber
                        ] = complaintNumber

                        statement[
                            ParentComplaintsTable.student
                        ] = request.studentId

                        statement[
                            ParentComplaintsTable.parentAccount
                        ] = authenticatedAccountId

                        statement[
                            ParentComplaintsTable.category
                        ] = category

                        statement[
                            ParentComplaintsTable.subject
                        ] = subject

                        statement[
                            ParentComplaintsTable.description
                        ] = description

                        statement[
                            ParentComplaintsTable.priority
                        ] = priority

                        statement[
                            ParentComplaintsTable.status
                        ] = ComplaintStatus.OPEN

                        statement[
                            ParentComplaintsTable.assignedAdminAccount
                        ] = null

                        statement[
                            ParentComplaintsTable.createdAt
                        ] = now

                        statement[
                            ParentComplaintsTable.updatedAt
                        ] = now

                        statement[
                            ParentComplaintsTable.lastActivityAt
                        ] = now

                        statement[
                            ParentComplaintsTable.resolvedAt
                        ] = null

                        statement[
                            ParentComplaintsTable.closedAt
                        ] = null
                    }
                    .value

            println(
                "[ComplaintRepository] Complaint created: " +
                        "complaintId=$complaintId, " +
                        "complaintNumber=$complaintNumber, " +
                        "studentId=${request.studentId}, " +
                        "parentAccountId=$authenticatedAccountId, " +
                        "tenantSchema=$tenantSchema"
            )

            findComplaintInCurrentTransaction(
                complaintId = complaintId,
                authenticatedAccountId = authenticatedAccountId,
                includeInternalReplies = false
            )
                ?: throw IllegalArgumentException(
                    "The complaint could not be loaded after creation."
                )
        }
    }

    fun findParentComplaints(
        tenantSchema: String,
        authenticatedAccountId: Int
    ): List<ParentComplaintResponse> {
        requireTenantAndAccount(
            tenantSchema = tenantSchema,
            accountId = authenticatedAccountId
        )

        return transaction {
            setTenantSchema(
                tenantSchema
            )

            ParentComplaintsTable
                .select(
                    ParentComplaintsTable.id
                )
                .where {
                    ParentComplaintsTable.parentAccount eq
                            authenticatedAccountId
                }
                .orderBy(
                    ParentComplaintsTable.lastActivityAt,
                    SortOrder.DESC
                )
                .mapNotNull { row ->
                    findComplaintInCurrentTransaction(
                        complaintId =
                            row[
                                ParentComplaintsTable.id
                            ].value,

                        authenticatedAccountId =
                            authenticatedAccountId,

                        includeInternalReplies =
                            false
                    )
                }
        }
    }

    fun findParentComplaint(
        tenantSchema: String,
        authenticatedAccountId: Int,
        complaintId: Int
    ): ParentComplaintResponse {
        requireTenantAndAccount(
            tenantSchema = tenantSchema,
            accountId = authenticatedAccountId
        )

        require(
            complaintId > 0
        ) {
            "A valid complaint ID is required."
        }

        return transaction {
            setTenantSchema(
                tenantSchema
            )

            val complaint =
                findComplaintInCurrentTransaction(
                    complaintId =
                        complaintId,

                    authenticatedAccountId =
                        authenticatedAccountId,

                    includeInternalReplies =
                        false
                )
                    ?: throw IllegalArgumentException(
                        "The complaint was not found."
                    )

            markAdminRepliesAsReadInCurrentTransaction(
                complaintId =
                    complaintId
            )

            complaint
        }
    }

    fun createParentReply(
        tenantSchema: String,
        authenticatedAccountId: Int,
        complaintId: Int,
        request: CreateComplaintReplyRequest
    ): ComplaintReplyResponse {
        requireTenantAndAccount(
            tenantSchema = tenantSchema,
            accountId = authenticatedAccountId
        )

        require(
            complaintId > 0
        ) {
            "A valid complaint ID is required."
        }

        val content =
            request.content
                .trim()

        require(
            content.isNotBlank()
        ) {
            "Reply content is required."
        }

        require(
            content.length <= 3000
        ) {
            "Reply content cannot exceed 3,000 characters."
        }

        return transaction {
            setTenantSchema(
                tenantSchema
            )

            val complaintRow =
                requireParentComplaintOwnerInCurrentTransaction(
                    complaintId =
                        complaintId,

                    authenticatedAccountId =
                        authenticatedAccountId
                )

            val currentStatus =
                complaintRow[
                    ParentComplaintsTable.status
                ]

            require(
                currentStatus !=
                        ComplaintStatus.CLOSED
            ) {
                "This complaint is closed and cannot receive replies."
            }

            val senderName =
                findAccountNameInCurrentTransaction(
                    accountId =
                        authenticatedAccountId
                )

            val now =
                LocalDateTime.now()

            val replyId =
                ComplaintRepliesTable
                    .insertAndGetId { statement ->
                        statement[
                            ComplaintRepliesTable.complaint
                        ] = complaintId

                        statement[
                            ComplaintRepliesTable.senderAccount
                        ] = authenticatedAccountId

                        statement[
                            ComplaintRepliesTable.senderRole
                        ] = ComplaintSenderRole.PARENT

                        statement[
                            ComplaintRepliesTable.content
                        ] = content

                        statement[
                            ComplaintRepliesTable.isInternal
                        ] = false

                        statement[
                            ComplaintRepliesTable.createdAt
                        ] = now

                        statement[
                            ComplaintRepliesTable.readAt
                        ] = null
                    }
                    .value

            ParentComplaintsTable.update(
                where = {
                    ParentComplaintsTable.id eq
                            complaintId
                }
            ) { statement ->
                statement[
                    ParentComplaintsTable.status
                ] = ComplaintStatus.IN_REVIEW

                statement[
                    ParentComplaintsTable.updatedAt
                ] = now

                statement[
                    ParentComplaintsTable.lastActivityAt
                ] = now

                statement[
                    ParentComplaintsTable.resolvedAt
                ] = null

                statement[
                    ParentComplaintsTable.closedAt
                ] = null
            }

            println(
                "[ComplaintRepository] Parent reply created: " +
                        "complaintId=$complaintId, " +
                        "replyId=$replyId, " +
                        "accountId=$authenticatedAccountId, " +
                        "tenantSchema=$tenantSchema"
            )

            ComplaintReplyResponse(
                id =
                    replyId,

                complaintId =
                    complaintId,

                senderAccountId =
                    authenticatedAccountId,

                senderName =
                    senderName,

                senderRole =
                    ComplaintSenderRole.PARENT,

                content =
                    content,

                isInternal =
                    false,

                createdAt =
                    now.toString(),

                readAt =
                    null,

                isMine =
                    true
            )
        }
    }

    fun findAdminComplaints(
        tenantSchema: String,
        authenticatedAdminAccountId: Int,
        statusFilter: String? = null
    ): List<ParentComplaintResponse> {
        requireTenantAndAccount(
            tenantSchema = tenantSchema,
            accountId = authenticatedAdminAccountId
        )

        val normalizedStatus =
            statusFilter
                ?.trim()
                ?.uppercase()
                ?.takeIf { status ->
                    status.isNotBlank()
                }

        if (normalizedStatus != null) {
            require(
                normalizedStatus in
                        ComplaintStatus.allowedValues
            ) {
                "A valid complaint status is required."
            }
        }

        return transaction {
            setTenantSchema(
                tenantSchema
            )

            val query =
                if (normalizedStatus == null) {
                    ParentComplaintsTable
                        .select(
                            ParentComplaintsTable.id
                        )
                } else {
                    ParentComplaintsTable
                        .select(
                            ParentComplaintsTable.id
                        )
                        .where {
                            ParentComplaintsTable.status eq
                                    normalizedStatus
                        }
                }

            query
                .orderBy(
                    ParentComplaintsTable.lastActivityAt,
                    SortOrder.DESC
                )
                .mapNotNull { row ->
                    findComplaintInCurrentTransaction(
                        complaintId =
                            row[
                                ParentComplaintsTable.id
                            ].value,

                        authenticatedAccountId =
                            authenticatedAdminAccountId,

                        includeInternalReplies =
                            true
                    )
                }
        }
    }

    fun findAdminComplaint(
        tenantSchema: String,
        authenticatedAdminAccountId: Int,
        complaintId: Int
    ): ParentComplaintResponse {
        requireTenantAndAccount(
            tenantSchema = tenantSchema,
            accountId = authenticatedAdminAccountId
        )

        require(
            complaintId > 0
        ) {
            "A valid complaint ID is required."
        }

        return transaction {
            setTenantSchema(
                tenantSchema
            )

            requireComplaintExistsInCurrentTransaction(
                complaintId =
                    complaintId
            )

            markParentRepliesAsReadInCurrentTransaction(
                complaintId =
                    complaintId
            )

            findComplaintInCurrentTransaction(
                complaintId =
                    complaintId,

                authenticatedAccountId =
                    authenticatedAdminAccountId,

                includeInternalReplies =
                    true
            )
                ?: throw IllegalArgumentException(
                    "The complaint was not found."
                )
        }
    }

    fun createAdminReply(
        tenantSchema: String,
        authenticatedAdminAccountId: Int,
        complaintId: Int,
        request: CreateAdminComplaintReplyRequest
    ): ComplaintReplyResponse {
        requireTenantAndAccount(
            tenantSchema = tenantSchema,
            accountId = authenticatedAdminAccountId
        )

        require(
            complaintId > 0
        ) {
            "A valid complaint ID is required."
        }

        val content =
            request.content
                .trim()

        require(
            content.isNotBlank()
        ) {
            "Reply content is required."
        }

        require(
            content.length <= 3000
        ) {
            "Reply content cannot exceed 3,000 characters."
        }

        val requestedNextStatus =
            request.nextStatus
                ?.trim()
                ?.uppercase()
                ?.takeIf { status ->
                    status.isNotBlank()
                }

        if (requestedNextStatus != null) {
            require(
                requestedNextStatus in
                        ComplaintStatus.allowedValues
            ) {
                "A valid next complaint status is required."
            }
        }

        return transaction {
            setTenantSchema(
                tenantSchema
            )

            val complaintRow =
                requireComplaintExistsInCurrentTransaction(
                    complaintId =
                        complaintId
                )

            val currentStatus =
                complaintRow[
                    ParentComplaintsTable.status
                ]

            require(
                currentStatus !=
                        ComplaintStatus.CLOSED
            ) {
                "This complaint is closed and cannot receive replies."
            }

            val senderName =
                findAccountNameInCurrentTransaction(
                    accountId =
                        authenticatedAdminAccountId
                )

            val now =
                LocalDateTime.now()

            val replyId =
                ComplaintRepliesTable
                    .insertAndGetId { statement ->
                        statement[
                            ComplaintRepliesTable.complaint
                        ] = complaintId

                        statement[
                            ComplaintRepliesTable.senderAccount
                        ] = authenticatedAdminAccountId

                        statement[
                            ComplaintRepliesTable.senderRole
                        ] = ComplaintSenderRole.ADMIN

                        statement[
                            ComplaintRepliesTable.content
                        ] = content

                        statement[
                            ComplaintRepliesTable.isInternal
                        ] = request.isInternal

                        statement[
                            ComplaintRepliesTable.createdAt
                        ] = now

                        statement[
                            ComplaintRepliesTable.readAt
                        ] = null
                    }
                    .value

            val nextStatus =
                when {
                    request.isInternal ->
                        currentStatus

                    requestedNextStatus != null ->
                        requestedNextStatus

                    else ->
                        ComplaintStatus.AWAITING_PARENT
                }

            ParentComplaintsTable.update(
                where = {
                    ParentComplaintsTable.id eq
                            complaintId
                }
            ) { statement ->
                statement[
                    ParentComplaintsTable.status
                ] = nextStatus

                statement[
                    ParentComplaintsTable.updatedAt
                ] = now

                statement[
                    ParentComplaintsTable.lastActivityAt
                ] = now

                if (
                    nextStatus ==
                    ComplaintStatus.RESOLVED
                ) {
                    statement[
                        ParentComplaintsTable.resolvedAt
                    ] = now
                }

                if (
                    nextStatus ==
                    ComplaintStatus.CLOSED
                ) {
                    statement[
                        ParentComplaintsTable.closedAt
                    ] = now
                }
            }

            println(
                "[ComplaintRepository] Admin reply created: " +
                        "complaintId=$complaintId, " +
                        "replyId=$replyId, " +
                        "adminAccountId=$authenticatedAdminAccountId, " +
                        "isInternal=${request.isInternal}, " +
                        "tenantSchema=$tenantSchema"
            )

            ComplaintReplyResponse(
                id =
                    replyId,

                complaintId =
                    complaintId,

                senderAccountId =
                    authenticatedAdminAccountId,

                senderName =
                    senderName,

                senderRole =
                    ComplaintSenderRole.ADMIN,

                content =
                    content,

                isInternal =
                    request.isInternal,

                createdAt =
                    now.toString(),

                readAt =
                    null,

                isMine =
                    true
            )
        }
    }

    fun updateComplaintStatus(
        tenantSchema: String,
        authenticatedAdminAccountId: Int,
        complaintId: Int,
        status: String
    ): ParentComplaintResponse {
        requireTenantAndAccount(
            tenantSchema = tenantSchema,
            accountId = authenticatedAdminAccountId
        )

        require(
            complaintId > 0
        ) {
            "A valid complaint ID is required."
        }

        val normalizedStatus =
            status
                .trim()
                .uppercase()

        require(
            normalizedStatus in
                    ComplaintStatus.allowedValues
        ) {
            "A valid complaint status is required."
        }

        return transaction {
            setTenantSchema(
                tenantSchema
            )

            requireComplaintExistsInCurrentTransaction(
                complaintId =
                    complaintId
            )

            val now =
                LocalDateTime.now()

            ParentComplaintsTable.update(
                where = {
                    ParentComplaintsTable.id eq
                            complaintId
                }
            ) { statement ->
                statement[
                    ParentComplaintsTable.status
                ] = normalizedStatus

                statement[
                    ParentComplaintsTable.updatedAt
                ] = now

                statement[
                    ParentComplaintsTable.lastActivityAt
                ] = now

                statement[
                    ParentComplaintsTable.resolvedAt
                ] =
                    if (
                        normalizedStatus ==
                        ComplaintStatus.RESOLVED
                    ) {
                        now
                    } else {
                        null
                    }

                statement[
                    ParentComplaintsTable.closedAt
                ] =
                    if (
                        normalizedStatus ==
                        ComplaintStatus.CLOSED
                    ) {
                        now
                    } else {
                        null
                    }
            }

            println(
                "[ComplaintRepository] Complaint status updated: " +
                        "complaintId=$complaintId, " +
                        "status=$normalizedStatus, " +
                        "adminAccountId=$authenticatedAdminAccountId, " +
                        "tenantSchema=$tenantSchema"
            )

            findComplaintInCurrentTransaction(
                complaintId =
                    complaintId,

                authenticatedAccountId =
                    authenticatedAdminAccountId,

                includeInternalReplies =
                    true
            )
                ?: throw IllegalArgumentException(
                    "The updated complaint could not be loaded."
                )
        }
    }

    fun assignComplaint(
        tenantSchema: String,
        authenticatedAdminAccountId: Int,
        complaintId: Int,
        request: AssignComplaintRequest
    ): ParentComplaintResponse {
        requireTenantAndAccount(
            tenantSchema = tenantSchema,
            accountId = authenticatedAdminAccountId
        )

        require(
            complaintId > 0
        ) {
            "A valid complaint ID is required."
        }

        if (
            request.adminAccountId != null
        ) {
            require(
                request.adminAccountId > 0
            ) {
                "A valid administrator account ID is required."
            }
        }

        return transaction {
            setTenantSchema(
                tenantSchema
            )

            requireComplaintExistsInCurrentTransaction(
                complaintId =
                    complaintId
            )

            if (
                request.adminAccountId != null
            ) {
                findAccountNameInCurrentTransaction(
                    accountId =
                        request.adminAccountId
                )
            }

            val now =
                LocalDateTime.now()

            ParentComplaintsTable.update(
                where = {
                    ParentComplaintsTable.id eq
                            complaintId
                }
            ) { statement ->
                statement[
                    ParentComplaintsTable.assignedAdminAccount
                ] = request.adminAccountId

                statement[
                    ParentComplaintsTable.status
                ] =
                    if (
                        request.adminAccountId != null
                    ) {
                        ComplaintStatus.ASSIGNED
                    } else {
                        ComplaintStatus.IN_REVIEW
                    }

                statement[
                    ParentComplaintsTable.updatedAt
                ] = now

                statement[
                    ParentComplaintsTable.lastActivityAt
                ] = now
            }

            println(
                "[ComplaintRepository] Complaint assignment updated: " +
                        "complaintId=$complaintId, " +
                        "assignedAdminAccountId=${request.adminAccountId}, " +
                        "updatedBy=$authenticatedAdminAccountId, " +
                        "tenantSchema=$tenantSchema"
            )

            findComplaintInCurrentTransaction(
                complaintId =
                    complaintId,

                authenticatedAccountId =
                    authenticatedAdminAccountId,

                includeInternalReplies =
                    true
            )
                ?: throw IllegalArgumentException(
                    "The assigned complaint could not be loaded."
                )
        }
    }

    private fun findComplaintInCurrentTransaction(
        complaintId: Int,
        authenticatedAccountId: Int,
        includeInternalReplies: Boolean
    ): ParentComplaintResponse? {
        val complaintRow =
            ParentComplaintsTable
                .select(
                    ParentComplaintsTable.id,
                    ParentComplaintsTable.complaintNumber,
                    ParentComplaintsTable.student,
                    ParentComplaintsTable.parentAccount,
                    ParentComplaintsTable.category,
                    ParentComplaintsTable.subject,
                    ParentComplaintsTable.description,
                    ParentComplaintsTable.priority,
                    ParentComplaintsTable.status,
                    ParentComplaintsTable.assignedAdminAccount,
                    ParentComplaintsTable.createdAt,
                    ParentComplaintsTable.updatedAt,
                    ParentComplaintsTable.lastActivityAt,
                    ParentComplaintsTable.resolvedAt,
                    ParentComplaintsTable.closedAt
                )
                .where {
                    ParentComplaintsTable.id eq
                            complaintId
                }
                .limit(1)
                .singleOrNull()
                ?: return null

        val studentId =
            complaintRow[
                ParentComplaintsTable.student
            ].value

        val parentAccountId =
            complaintRow[
                ParentComplaintsTable.parentAccount
            ].value

        if (!includeInternalReplies) {
            require(
                parentAccountId ==
                        authenticatedAccountId
            ) {
                "You cannot access this complaint."
            }
        }

        val studentName =
            findStudentNameInCurrentTransaction(
                studentId =
                    studentId
            )

        val assignedAdminAccountId =
            complaintRow[
                ParentComplaintsTable.assignedAdminAccount
            ]?.value

        val assignedAdminName =
            assignedAdminAccountId
                ?.let { accountId ->
                    findAccountNameInCurrentTransaction(
                        accountId =
                            accountId
                    )
                }

        val replies =
            findRepliesInCurrentTransaction(
                complaintId =
                    complaintId,

                authenticatedAccountId =
                    authenticatedAccountId,

                includeInternalReplies =
                    includeInternalReplies
            )

        val latestVisibleReply =
            replies.maxByOrNull { reply ->
                reply.createdAt
            }

        val unreadReplyCount =
            replies.count { reply ->
                reply.readAt == null &&
                        reply.senderAccountId !=
                        authenticatedAccountId
            }

        return ParentComplaintResponse(
            id =
                complaintRow[
                    ParentComplaintsTable.id
                ].value,

            complaintNumber =
                complaintRow[
                    ParentComplaintsTable.complaintNumber
                ],

            studentId =
                studentId,

            studentName =
                studentName,

            parentAccountId =
                parentAccountId,

            category =
                complaintRow[
                    ParentComplaintsTable.category
                ],

            subject =
                complaintRow[
                    ParentComplaintsTable.subject
                ],

            description =
                complaintRow[
                    ParentComplaintsTable.description
                ],

            priority =
                complaintRow[
                    ParentComplaintsTable.priority
                ],

            status =
                complaintRow[
                    ParentComplaintsTable.status
                ],

            assignedAdminAccountId =
                assignedAdminAccountId,

            assignedAdminName =
                assignedAdminName,

            latestReply =
                latestVisibleReply?.content,

            latestReplyAt =
                latestVisibleReply?.createdAt,

            unreadReplyCount =
                unreadReplyCount,

            createdAt =
                complaintRow[
                    ParentComplaintsTable.createdAt
                ].toString(),

            updatedAt =
                complaintRow[
                    ParentComplaintsTable.updatedAt
                ].toString(),

            resolvedAt =
                complaintRow[
                    ParentComplaintsTable.resolvedAt
                ]?.toString(),

            closedAt =
                complaintRow[
                    ParentComplaintsTable.closedAt
                ]?.toString(),

            replies =
                replies
        )
    }

    private fun findRepliesInCurrentTransaction(
        complaintId: Int,
        authenticatedAccountId: Int,
        includeInternalReplies: Boolean
    ): List<ComplaintReplyResponse> {
        val query =
            if (includeInternalReplies) {
                ComplaintRepliesTable
                    .select(
                        ComplaintRepliesTable.id,
                        ComplaintRepliesTable.complaint,
                        ComplaintRepliesTable.senderAccount,
                        ComplaintRepliesTable.senderRole,
                        ComplaintRepliesTable.content,
                        ComplaintRepliesTable.isInternal,
                        ComplaintRepliesTable.createdAt,
                        ComplaintRepliesTable.readAt
                    )
                    .where {
                        ComplaintRepliesTable.complaint eq
                                complaintId
                    }
            } else {
                ComplaintRepliesTable
                    .select(
                        ComplaintRepliesTable.id,
                        ComplaintRepliesTable.complaint,
                        ComplaintRepliesTable.senderAccount,
                        ComplaintRepliesTable.senderRole,
                        ComplaintRepliesTable.content,
                        ComplaintRepliesTable.isInternal,
                        ComplaintRepliesTable.createdAt,
                        ComplaintRepliesTable.readAt
                    )
                    .where {
                        (
                                ComplaintRepliesTable.complaint eq
                                        complaintId
                                ) and
                                (
                                        ComplaintRepliesTable.isInternal eq
                                                false
                                        )
                    }
            }

        return query
            .orderBy(
                ComplaintRepliesTable.createdAt,
                SortOrder.ASC
            )
            .map { row ->
                val senderAccountId =
                    row[
                        ComplaintRepliesTable.senderAccount
                    ].value

                ComplaintReplyResponse(
                    id =
                        row[
                            ComplaintRepliesTable.id
                        ].value,

                    complaintId =
                        row[
                            ComplaintRepliesTable.complaint
                        ].value,

                    senderAccountId =
                        senderAccountId,

                    senderName =
                        findAccountNameInCurrentTransaction(
                            accountId =
                                senderAccountId
                        ),

                    senderRole =
                        row[
                            ComplaintRepliesTable.senderRole
                        ],

                    content =
                        row[
                            ComplaintRepliesTable.content
                        ],

                    isInternal =
                        row[
                            ComplaintRepliesTable.isInternal
                        ],

                    createdAt =
                        row[
                            ComplaintRepliesTable.createdAt
                        ].toString(),

                    readAt =
                        row[
                            ComplaintRepliesTable.readAt
                        ]?.toString(),

                    isMine =
                        senderAccountId ==
                                authenticatedAccountId
                )
            }
    }

    private fun requireParentComplaintOwnerInCurrentTransaction(
        complaintId: Int,
        authenticatedAccountId: Int
    ) =
        ParentComplaintsTable
            .select(
                ParentComplaintsTable.id,
                ParentComplaintsTable.parentAccount,
                ParentComplaintsTable.status
            )
            .where {
                (
                        ParentComplaintsTable.id eq
                                complaintId
                        ) and
                        (
                                ParentComplaintsTable.parentAccount eq
                                        authenticatedAccountId
                                )
            }
            .limit(1)
            .singleOrNull()
            ?: throw IllegalArgumentException(
                "The complaint was not found or does not belong to your account."
            )

    private fun requireComplaintExistsInCurrentTransaction(
        complaintId: Int
    ) =
        ParentComplaintsTable
            .select(
                ParentComplaintsTable.id,
                ParentComplaintsTable.status
            )
            .where {
                ParentComplaintsTable.id eq
                        complaintId
            }
            .limit(1)
            .singleOrNull()
            ?: throw IllegalArgumentException(
                "The complaint was not found."
            )

    private fun findAccountNameInCurrentTransaction(
        accountId: Int
    ): String {
        return AccountTable
            .select(
                AccountTable.fullName
            )
            .where {
                AccountTable.id eq
                        accountId
            }
            .limit(1)
            .singleOrNull()
            ?.get(
                AccountTable.fullName
            )
            ?: throw IllegalArgumentException(
                "The account was not found."
            )
    }

    private fun findStudentNameInCurrentTransaction(
        studentId: Int
    ): String {
        val studentRow =
            StudentsTable
                .select(
                    StudentsTable.user
                )
                .where {
                    StudentsTable.id eq
                            studentId
                }
                .limit(1)
                .singleOrNull()
                ?: return "Unknown student"

        val studentAccountId =
            studentRow[
                StudentsTable.user
            ].value

        return AccountTable
            .select(
                AccountTable.fullName
            )
            .where {
                AccountTable.id eq
                        studentAccountId
            }
            .limit(1)
            .singleOrNull()
            ?.get(
                AccountTable.fullName
            )
            ?: "Unknown student"
    }

    private fun markAdminRepliesAsReadInCurrentTransaction(
        complaintId: Int
    ) {
        ComplaintRepliesTable.update(
            where = {
                (
                        ComplaintRepliesTable.complaint eq
                                complaintId
                        ) and
                        (
                                ComplaintRepliesTable.senderRole eq
                                        ComplaintSenderRole.ADMIN
                                ) and
                        (
                                ComplaintRepliesTable.isInternal eq
                                        false
                                ) and
                        ComplaintRepliesTable.readAt.isNull()
            }
        ) { statement ->
            statement[
                ComplaintRepliesTable.readAt
            ] = LocalDateTime.now()
        }
    }

    private fun markParentRepliesAsReadInCurrentTransaction(
        complaintId: Int
    ) {
        ComplaintRepliesTable.update(
            where = {
                (
                        ComplaintRepliesTable.complaint eq
                                complaintId
                        ) and
                        (
                                ComplaintRepliesTable.senderRole eq
                                        ComplaintSenderRole.PARENT
                                ) and
                        ComplaintRepliesTable.readAt.isNull()
            }
        ) { statement ->
            statement[
                ComplaintRepliesTable.readAt
            ] = LocalDateTime.now()
        }
    }

    private fun requireTenantAndAccount(
        tenantSchema: String,
        accountId: Int
    ) {
        require(
            tenantSchema.isNotBlank()
        ) {
            "Tenant schema is required."
        }

        require(
            accountId > 0
        ) {
            "A valid authenticated account ID is required."
        }
    }

    private fun generateComplaintNumber(): String {
        val datePart =
            LocalDateTime.now()
                .format(
                    DateTimeFormatter.ofPattern(
                        "yyyyMMdd"
                    )
                )

        val randomPart =
            UUID.randomUUID()
                .toString()
                .replace(
                    "-",
                    ""
                )
                .take(8)
                .uppercase()

        return "CMP-$datePart-$randomPart"
    }
}