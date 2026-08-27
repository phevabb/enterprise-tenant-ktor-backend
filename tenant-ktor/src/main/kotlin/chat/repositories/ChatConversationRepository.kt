package chat.repositories



import chat.models.ChatConversationResponse
import chat.server.ChatConnectionManager
import chat.tables.ChatConversationsTable
import chat.tables.ChatMessagesTable
import com.example.account.AccountTable
import com.example.academics.repos.setTenantSchema
import com.example.staff.tables.StaffTable
import com.example.student.StudentsTable
import com.example.student.tables.NewGradeClassTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

object ChatConversationRepository {

    fun findConversationsForAccount(
        tenantSchema: String,
        authenticatedAccountId: Int
    ): List<ChatConversationResponse> {

        require(tenantSchema.isNotBlank()) {
            "Tenant schema is required."
        }

        require(authenticatedAccountId > 0) {
            "A valid authenticated account ID is required."
        }

        return transaction {
            setTenantSchema(
                tenantSchema
            )

            ChatConversationsTable
                .select(
                    ChatConversationsTable.id,
                    ChatConversationsTable.student,
                    ChatConversationsTable.parentAccount,
                    ChatConversationsTable.teacherAccount,
                    ChatConversationsTable.createdAt,
                    ChatConversationsTable.updatedAt,
                    ChatConversationsTable.lastMessageAt,
                    ChatConversationsTable.isClosed
                )
                .where {
                    (
                            ChatConversationsTable.parentAccount eq
                                    authenticatedAccountId
                            ) or
                            (
                                    ChatConversationsTable.teacherAccount eq
                                            authenticatedAccountId
                                    )
                }
                .orderBy(
                    ChatConversationsTable.updatedAt,
                    SortOrder.DESC
                )
                .mapNotNull { conversationRow ->

                    val conversationId =
                        conversationRow[
                            ChatConversationsTable.id
                        ].value

                    val studentId =
                        conversationRow[
                            ChatConversationsTable.student
                        ].value

                    val parentAccountId =
                        conversationRow[
                            ChatConversationsTable.parentAccount
                        ].value

                    val teacherAccountId =
                        conversationRow[
                            ChatConversationsTable.teacherAccount
                        ].value

                    val createdAt =
                        conversationRow[
                            ChatConversationsTable.createdAt
                        ]

                    val storedLastMessageAt =
                        conversationRow[
                            ChatConversationsTable.lastMessageAt
                        ]

                    /*
                     * Load the student profile.
                     */
                    val studentRow =
                        StudentsTable
                            .select(
                                StudentsTable.id,
                                StudentsTable.user,
                                StudentsTable.currentNewGradeClass
                            )
                            .where {
                                StudentsTable.id eq
                                        studentId
                            }
                            .limit(1)
                            .singleOrNull()
                            ?: return@mapNotNull null

                    val studentAccountId =
                        studentRow[
                            StudentsTable.user
                        ].value

                    val classId =
                        studentRow[
                            StudentsTable.currentNewGradeClass
                        ]?.value

                    /*
                     * Load the student account details.
                     */
                    val studentAccountRow =
                        AccountTable
                            .select(
                                AccountTable.id,
                                AccountTable.userId,
                                AccountTable.fullName,
                                AccountTable.isActive
                            )
                            .where {
                                AccountTable.id eq
                                        studentAccountId
                            }
                            .limit(1)
                            .singleOrNull()

                    val studentName =
                        studentAccountRow
                            ?.get(
                                AccountTable.fullName
                            )
                            ?: "Unknown student"

                    /*
                     * Load current class details.
                     */
                    val className =
                        if (classId != null) {
                            NewGradeClassTable
                                .select(
                                    NewGradeClassTable.name
                                )
                                .where {
                                    NewGradeClassTable.id eq
                                            classId
                                }
                                .limit(1)
                                .singleOrNull()
                                ?.get(
                                    NewGradeClassTable.name
                                )
                                ?: "Class not found"
                        } else {
                            "Class not assigned"
                        }

                    /*
                     * Load the parent-side participant details.
                     *
                     * In your current system, the student account
                     * represents the parent-side portal participant.
                     */
                    val parentAccountRow =
                        AccountTable
                            .select(
                                AccountTable.id,
                                AccountTable.userId,
                                AccountTable.fullName,
                                AccountTable.isActive
                            )
                            .where {
                                AccountTable.id eq
                                        parentAccountId
                            }
                            .limit(1)
                            .singleOrNull()

                    val parentName =
                        parentAccountRow
                            ?.get(
                                AccountTable.fullName
                            )
                            ?: studentName

                    /*
                     * Load the teacher account details.
                     */
                    val teacherAccountRow =
                        AccountTable
                            .select(
                                AccountTable.id,
                                AccountTable.userId,
                                AccountTable.fullName,
                                AccountTable.isActive
                            )
                            .where {
                                AccountTable.id eq
                                        teacherAccountId
                            }
                            .limit(1)
                            .singleOrNull()

                    val teacherName =
                        teacherAccountRow
                            ?.get(
                                AccountTable.fullName
                            )
                            ?: "Class Teacher"

                    /*
                     * Load the latest message.
                     */
                    val latestMessageRow =
                        ChatMessagesTable
                            .select(
                                ChatMessagesTable.id,
                                ChatMessagesTable.content,
                                ChatMessagesTable.createdAt
                            )
                            .where {
                                ChatMessagesTable.conversation eq
                                        conversationId
                            }
                            .orderBy(
                                ChatMessagesTable.createdAt,
                                SortOrder.DESC
                            )
                            .limit(1)
                            .singleOrNull()

                    val lastMessage =
                        latestMessageRow
                            ?.get(
                                ChatMessagesTable.content
                            )

                    val lastMessageAt =
                        latestMessageRow
                            ?.get(
                                ChatMessagesTable.createdAt
                            )
                            ?.toString()
                            ?: storedLastMessageAt
                                ?.toString()

                    /*
                     * Count messages sent by the other participant
                     * that have not yet been read.
                     */
                    val unreadCount =
                        ChatMessagesTable
                            .select(
                                ChatMessagesTable.id
                            )
                            .where {
                                (
                                        ChatMessagesTable.conversation eq
                                                conversationId
                                        ) and
                                        (
                                                ChatMessagesTable.senderAccount neq
                                                        authenticatedAccountId
                                                ) and
                                        ChatMessagesTable.readAt.isNull()
                            }
                            .count()
                            .toInt()

                    ChatConversationResponse(
                        id =
                            conversationId,

                        studentId =
                            studentId,

                        studentName =
                            studentName,

                        classId =
                            classId ?: 0,

                        className =
                            className,

                        parentAccountId =
                            parentAccountId,

                        parentName =
                            parentName,

                        teacherAccountId =
                            teacherAccountId,

                        teacherName =
                            teacherName,

                        teacherOnline =
                            ChatConnectionManager.isConnected(
                                teacherAccountId
                            ),

                        parentOnline =
                            ChatConnectionManager.isConnected(
                                parentAccountId
                            ),

                        lastMessage =
                            lastMessage,

                        lastMessageAt =
                            lastMessageAt,

                        unreadCount =
                            unreadCount,

                        createdAt =
                            createdAt.toString()
                    )
                }
        }
    }

    fun findOrCreateParentTeacherConversation(
        tenantSchema: String,
        authenticatedAccountId: Int,
        studentId: Int
    ): ChatConversationResponse {

        require(tenantSchema.isNotBlank()) {
            "Tenant schema is required."
        }

        require(authenticatedAccountId > 0) {
            "A valid authenticated account ID is required."
        }

        require(studentId > 0) {
            "A valid student ID is required."
        }

        return transaction {
            setTenantSchema(
                tenantSchema
            )

            println(
                "[ChatConversationRepository] Opening conversation: " +
                        "authenticatedAccountId=$authenticatedAccountId, " +
                        "studentId=$studentId, " +
                        "tenantSchema=$tenantSchema"
            )

            /*
             * Load the selected student.
             *
             * StudentsTable.id is the student profile ID.
             * StudentsTable.user is the student portal AccountTable.id.
             */
            val studentRow =
                StudentsTable
                    .select(
                        StudentsTable.id,
                        StudentsTable.user,
                        StudentsTable.currentNewGradeClass,
                        StudentsTable.isGraduated
                    )
                    .where {
                        StudentsTable.id eq
                                studentId
                    }
                    .limit(1)
                    .singleOrNull()
                    ?: throw IllegalArgumentException(
                        "The student profile was not found."
                    )

            require(
                !studentRow[
                    StudentsTable.isGraduated
                ]
            ) {
                "A conversation cannot be created for a graduated student."
            }

            val studentAccountId =
                studentRow[
                    StudentsTable.user
                ].value

            val classId =
                studentRow[
                    StudentsTable.currentNewGradeClass
                ]?.value
                    ?: throw IllegalArgumentException(
                        "The student has not been assigned to a current class."
                    )

            println(
                "[ChatConversationRepository] Student resolved: " +
                        "studentId=$studentId, " +
                        "studentAccountId=$studentAccountId, " +
                        "classId=$classId"
            )

            /*
             * Load the student-side portal account.
             *
             * In the current system, this account represents the parent-side
             * participant in the parent communication portal.
             */
            val studentAccountRow =
                AccountTable
                    .select(
                        AccountTable.id,
                        AccountTable.fullName,
                        AccountTable.isActive
                    )
                    .where {
                        AccountTable.id eq
                                studentAccountId
                    }
                    .limit(1)
                    .singleOrNull()
                    ?: throw IllegalArgumentException(
                        "The student portal account was not found."
                    )

            require(
                studentAccountRow[
                    AccountTable.isActive
                ]
            ) {
                "The student portal account is inactive."
            }

            val studentName =
                studentAccountRow[
                    AccountTable.fullName
                ]

            /*
             * Load the student's current class.
             */
            val classRow =
                NewGradeClassTable
                    .select(
                        NewGradeClassTable.id,
                        NewGradeClassTable.name,
                        NewGradeClassTable.isActive
                    )
                    .where {
                        NewGradeClassTable.id eq
                                classId
                    }
                    .limit(1)
                    .singleOrNull()
                    ?: throw IllegalArgumentException(
                        "The student's current class was not found."
                    )

            require(
                classRow[
                    NewGradeClassTable.isActive
                ]
            ) {
                "The student's current class is inactive."
            }

            val className =
                classRow[
                    NewGradeClassTable.name
                ]

            /*
             * Find the staff member assigned to the student's class.
             *
             * StaffTable.user stores AccountTable.id.
             */
            val assignedStaffRow =
                StaffTable
                    .select(
                        StaffTable.id,
                        StaffTable.user,
                        StaffTable.assignedClass
                    )
                    .where {
                        (
                                StaffTable.assignedClass eq
                                        classId
                                ) and
                                StaffTable.user.isNotNull()
                    }
                    .orderBy(
                        StaffTable.id,
                        SortOrder.ASC
                    )
                    .limit(1)
                    .singleOrNull()
                    ?: throw IllegalArgumentException(
                        "No staff member has been assigned to $className."
                    )

            val assignedStaffId =
                assignedStaffRow[
                    StaffTable.id
                ].value

            val teacherAccountId =
                assignedStaffRow[
                    StaffTable.user
                ]?.value
                    ?: throw IllegalArgumentException(
                        "The assigned staff member does not have a portal account."
                    )

            println(
                "[ChatConversationRepository] Assigned staff resolved: " +
                        "staffId=$assignedStaffId, " +
                        "teacherAccountId=$teacherAccountId, " +
                        "classId=$classId"
            )

            /*
             * Load the assigned staff account.
             */
            val teacherAccountRow =
                AccountTable
                    .select(
                        AccountTable.id,
                        AccountTable.fullName,
                        AccountTable.isActive
                    )
                    .where {
                        AccountTable.id eq
                                teacherAccountId
                    }
                    .limit(1)
                    .singleOrNull()
                    ?: throw IllegalArgumentException(
                        "The assigned staff account was not found."
                    )

            require(
                teacherAccountRow[
                    AccountTable.isActive
                ]
            ) {
                "The assigned staff account is inactive."
            }

            val teacherName =
                teacherAccountRow[
                    AccountTable.fullName
                ]

            /*
             * Participant authorization.
             *
             * Either participant may find or create this conversation:
             *
             * 1. The student-side parent portal account
             * 2. The staff account assigned to the student's class
             */
            val isParentSideAccount =
                authenticatedAccountId ==
                        studentAccountId

            val isAssignedStaffAccount =
                authenticatedAccountId ==
                        teacherAccountId

            println(
                "[ChatConversationRepository] Participant authorization: " +
                        "authenticatedAccountId=$authenticatedAccountId, " +
                        "studentAccountId=$studentAccountId, " +
                        "teacherAccountId=$teacherAccountId, " +
                        "isParentSideAccount=$isParentSideAccount, " +
                        "isAssignedStaffAccount=$isAssignedStaffAccount"
            )

            require(
                isParentSideAccount ||
                        isAssignedStaffAccount
            ) {
                "You can only start conversations for students in your assigned class."
            }

            /*
             * Participant IDs must not depend on who initiated the request.
             *
             * Parent side:
             * Student portal account
             *
             * Teacher side:
             * Staff account assigned to the class
             */
            val parentAccountId =
                studentAccountId

            /*
             * Find an existing conversation between these exact
             * participants regarding this student.
             */
            val existingConversation =
                ChatConversationsTable
                    .select(
                        ChatConversationsTable.id,
                        ChatConversationsTable.createdAt,
                        ChatConversationsTable.lastMessageAt,
                        ChatConversationsTable.isClosed
                    )
                    .where {
                        (
                                ChatConversationsTable.student eq
                                        studentId
                                ) and
                                (
                                        ChatConversationsTable.parentAccount eq
                                                parentAccountId
                                        ) and
                                (
                                        ChatConversationsTable.teacherAccount eq
                                                teacherAccountId
                                        )
                    }
                    .limit(1)
                    .singleOrNull()

            val conversationId: Int
            val createdAt: LocalDateTime
            val lastMessageAt: LocalDateTime?

            if (existingConversation != null) {
                require(
                    !existingConversation[
                        ChatConversationsTable.isClosed
                    ]
                ) {
                    "This conversation has been closed."
                }

                conversationId =
                    existingConversation[
                        ChatConversationsTable.id
                    ].value

                createdAt =
                    existingConversation[
                        ChatConversationsTable.createdAt
                    ]

                lastMessageAt =
                    existingConversation[
                        ChatConversationsTable.lastMessageAt
                    ]

                println(
                    "[ChatConversationRepository] Existing conversation found: " +
                            "conversationId=$conversationId, " +
                            "studentId=$studentId, " +
                            "parentAccountId=$parentAccountId, " +
                            "teacherAccountId=$teacherAccountId"
                )
            } else {
                val now =
                    LocalDateTime.now()

                conversationId =
                    ChatConversationsTable
                        .insertAndGetId { statement ->
                            statement[
                                ChatConversationsTable.student
                            ] = studentId

                            statement[
                                ChatConversationsTable.parentAccount
                            ] = parentAccountId

                            statement[
                                ChatConversationsTable.teacherAccount
                            ] = teacherAccountId

                            statement[
                                ChatConversationsTable.createdAt
                            ] = now

                            statement[
                                ChatConversationsTable.updatedAt
                            ] = now

                            statement[
                                ChatConversationsTable.lastMessageAt
                            ] = null

                            statement[
                                ChatConversationsTable.isClosed
                            ] = false
                        }
                        .value

                createdAt =
                    now

                lastMessageAt =
                    null

                println(
                    "[ChatConversationRepository] New conversation created: " +
                            "conversationId=$conversationId, " +
                            "studentId=$studentId, " +
                            "parentAccountId=$parentAccountId, " +
                            "teacherAccountId=$teacherAccountId"
                )
            }

            /*
             * Load the latest message, if one exists.
             */
            val latestMessageRow =
                ChatMessagesTable
                    .select(
                        ChatMessagesTable.content,
                        ChatMessagesTable.createdAt
                    )
                    .where {
                        ChatMessagesTable.conversation eq
                                conversationId
                    }
                    .orderBy(
                        ChatMessagesTable.createdAt,
                        SortOrder.DESC
                    )
                    .limit(1)
                    .singleOrNull()

            val latestMessage =
                latestMessageRow
                    ?.get(
                        ChatMessagesTable.content
                    )

            val resolvedLastMessageAt =
                latestMessageRow
                    ?.get(
                        ChatMessagesTable.createdAt
                    )
                    ?: lastMessageAt

            /*
             * Count unread messages sent by the other participant.
             */
            val unreadCount =
                ChatMessagesTable
                    .select(
                        ChatMessagesTable.id
                    )
                    .where {
                        (
                                ChatMessagesTable.conversation eq
                                        conversationId
                                ) and
                                (
                                        ChatMessagesTable.senderAccount neq
                                                authenticatedAccountId
                                        ) and
                                ChatMessagesTable.readAt.isNull()
                    }
                    .count()
                    .toInt()

            ChatConversationResponse(
                id =
                    conversationId,

                studentId =
                    studentId,

                studentName =
                    studentName,

                classId =
                    classId,

                className =
                    className,

                parentAccountId =
                    parentAccountId,

                parentName =
                    studentName,

                teacherAccountId =
                    teacherAccountId,

                teacherName =
                    teacherName,

                teacherOnline =
                    ChatConnectionManager.isConnected(
                        teacherAccountId
                    ),

                parentOnline =
                    ChatConnectionManager.isConnected(
                        parentAccountId
                    ),

                lastMessage =
                    latestMessage,

                lastMessageAt =
                    resolvedLastMessageAt
                        ?.toString(),

                unreadCount =
                    unreadCount,

                createdAt =
                    createdAt.toString()
            )
        }
    }
}