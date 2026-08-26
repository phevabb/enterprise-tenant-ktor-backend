package chat.repositories

import chat.models.ChatMessageResponse
import chat.models.DeleteChatMessageResult
import chat.tables.ChatConversationsTable
import chat.tables.ChatMessagesTable
import com.example.account.AccountTable
import com.example.student.repos.setTenantSchema
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

object ChatRepository {

    data class ConversationParticipants(
        val conversationId: Int,
        val parentAccountId: Int,
        val teacherAccountId: Int,
        val studentId: Int,
        val isClosed: Boolean
    )

    fun deleteMessage(
        tenantSchema: String,
        authenticatedAccountId: Int,
        messageId: Int
    ): DeleteChatMessageResult {

        require(
            tenantSchema.isNotBlank()
        ) {
            "Tenant schema is required."
        }

        require(
            authenticatedAccountId > 0
        ) {
            "A valid authenticated account ID is required."
        }

        require(
            messageId > 0
        ) {
            "A valid message ID is required."
        }

        return transaction {
            setTenantSchema(
                tenantSchema
            )

            println(
                "[ChatRepository] Permanent message deletion requested: " +
                        "accountId=$authenticatedAccountId, " +
                        "messageId=$messageId, " +
                        "tenantSchema=$tenantSchema"
            )

            val messageRow =
                ChatMessagesTable
                    .select(
                        ChatMessagesTable.id,
                        ChatMessagesTable.conversation,
                        ChatMessagesTable.senderAccount,
                        ChatMessagesTable.createdAt
                    )
                    .where {
                        ChatMessagesTable.id eq
                                messageId
                    }
                    .limit(1)
                    .singleOrNull()
                    ?: throw IllegalArgumentException(
                        "The message was not found."
                    )

            val conversationId =
                messageRow[
                    ChatMessagesTable.conversation
                ].value

            val senderAccountId =
                messageRow[
                    ChatMessagesTable.senderAccount
                ].value

            require(
                senderAccountId ==
                        authenticatedAccountId
            ) {
                "You can only delete messages that you sent."
            }

            val conversationRow =
                ChatConversationsTable
                    .select(
                        ChatConversationsTable.id,
                        ChatConversationsTable.parentAccount,
                        ChatConversationsTable.teacherAccount
                    )
                    .where {
                        ChatConversationsTable.id eq
                                conversationId
                    }
                    .limit(1)
                    .singleOrNull()
                    ?: throw IllegalArgumentException(
                        "The conversation was not found."
                    )

            val parentAccountId =
                conversationRow[
                    ChatConversationsTable.parentAccount
                ].value

            val teacherAccountId =
                conversationRow[
                    ChatConversationsTable.teacherAccount
                ].value

            val isConversationParticipant =
                authenticatedAccountId ==
                        parentAccountId ||
                        authenticatedAccountId ==
                        teacherAccountId

            require(
                isConversationParticipant
            ) {
                "You cannot delete messages from this conversation."
            }

            val deletedAt =
                LocalDateTime.now()

            val deletedRows =
                ChatMessagesTable.deleteWhere {
                    (
                            ChatMessagesTable.id eq
                                    messageId
                            ) and
                            (
                                    ChatMessagesTable.senderAccount eq
                                            authenticatedAccountId
                                    )
                }

            require(
                deletedRows == 1
            ) {
                "The message could not be deleted."
            }

            val latestRemainingMessageRow =
                ChatMessagesTable
                    .select(
                        ChatMessagesTable.id,
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

            val now =
                LocalDateTime.now()

            ChatConversationsTable.update(
                where = {
                    ChatConversationsTable.id eq
                            conversationId
                }
            ) { statement ->
                statement[
                    ChatConversationsTable.updatedAt
                ] = now

                statement[
                    ChatConversationsTable.lastMessageAt
                ] =
                    latestRemainingMessageRow
                        ?.get(
                            ChatMessagesTable.createdAt
                        )
            }

            println(
                "[ChatRepository] Message permanently deleted: " +
                        "accountId=$authenticatedAccountId, " +
                        "messageId=$messageId, " +
                        "conversationId=$conversationId, " +
                        "tenantSchema=$tenantSchema"
            )

            DeleteChatMessageResult(
                messageId =
                    messageId,

                conversationId =
                    conversationId,

                deletedAt =
                    deletedAt.toString(),

                deletedByAccountId =
                    authenticatedAccountId,

                parentAccountId =
                    parentAccountId,

                teacherAccountId =
                    teacherAccountId
            )
        }
    }

    fun findParticipants(
        tenantSchema: String,
        conversationId: Int
    ): ConversationParticipants? {

        require(tenantSchema.isNotBlank()) {
            "Tenant schema is required."
        }

        require(conversationId > 0) {
            "A valid conversation ID is required."
        }

        return transaction {
            setTenantSchema(
                tenantSchema
            )

            findParticipantsInCurrentTransaction(
                conversationId = conversationId
            )
        }
    }

    fun createMessage(
        tenantSchema: String,
        conversationId: Int,
        senderAccountId: Int,
        content: String
    ): ChatMessageResponse {

        require(tenantSchema.isNotBlank()) {
            "Tenant schema is required."
        }

        require(conversationId > 0) {
            "A valid conversation ID is required."
        }

        require(senderAccountId > 0) {
            "A valid sender account ID is required."
        }

        val normalizedContent =
            content.trim()

        require(normalizedContent.isNotBlank()) {
            "Message cannot be empty."
        }

        require(normalizedContent.length <= 2000) {
            "Message cannot exceed 2,000 characters."
        }

        return transaction {
            setTenantSchema(
                tenantSchema
            )

            val participants =
                findParticipantsInCurrentTransaction(
                    conversationId = conversationId
                )
                    ?: throw IllegalArgumentException(
                        "Conversation was not found."
                    )

            require(
                senderAccountId ==
                        participants.parentAccountId ||
                        senderAccountId ==
                        participants.teacherAccountId
            ) {
                "You are not a participant in this conversation."
            }

            require(!participants.isClosed) {
                "This conversation is closed."
            }

            val senderName =
                AccountTable
                    .select(
                        AccountTable.fullName
                    )
                    .where {
                        AccountTable.id eq
                                senderAccountId
                    }
                    .limit(1)
                    .singleOrNull()
                    ?.get(
                        AccountTable.fullName
                    )
                    ?: throw IllegalArgumentException(
                        "The sender account was not found."
                    )

            val now =
                LocalDateTime.now()

            val messageId =
                ChatMessagesTable
                    .insertAndGetId { statement ->

                        statement[
                            ChatMessagesTable.conversation
                        ] = conversationId

                        statement[
                            ChatMessagesTable.senderAccount
                        ] = senderAccountId

                        statement[
                            ChatMessagesTable.content
                        ] = normalizedContent

                        statement[
                            ChatMessagesTable.createdAt
                        ] = now

                        statement[
                            ChatMessagesTable.readAt
                        ] = null
                    }
                    .value

            ChatConversationsTable
                .update({
                    ChatConversationsTable.id eq
                            conversationId
                }) { statement ->

                    statement[
                        ChatConversationsTable.updatedAt
                    ] = now

                    statement[
                        ChatConversationsTable.lastMessageAt
                    ] = now
                }

            ChatMessageResponse(
                id = messageId,
                conversationId = conversationId,
                senderAccountId = senderAccountId,
                senderName = senderName,
                content = normalizedContent,
                createdAt = now.toString(),
                readAt = null,
                isMine = true
            )
        }
    }

    fun findMessages(
        tenantSchema: String,
        conversationId: Int,
        authenticatedAccountId: Int,
        limit: Int = 50
    ): List<ChatMessageResponse> {

        require(
            tenantSchema.isNotBlank()
        ) {
            "Tenant schema is required."
        }

        require(
            conversationId > 0
        ) {
            "A valid conversation ID is required."
        }

        require(
            authenticatedAccountId > 0
        ) {
            "A valid authenticated account ID is required."
        }

        val normalizedLimit =
            limit.coerceIn(
                minimumValue = 1,
                maximumValue = 100
            )

        return transaction {
            setTenantSchema(
                tenantSchema
            )

            requireParticipantInCurrentTransaction(
                conversationId =
                    conversationId,

                accountId =
                    authenticatedAccountId
            )

            /*
             * ChatMessagesTable now has two account references:
             *
             * sender_account_id
             * deleted_by_account_id
             *
             * Therefore, an implicit innerJoin(AccountTable) is ambiguous.
             * This explicitly joins through sender_account_id.
             */
            val messagesWithSender =
                ChatMessagesTable.join(
                    otherTable =
                        AccountTable,

                    joinType =
                        JoinType.INNER,

                    onColumn =
                        ChatMessagesTable.senderAccount,

                    otherColumn =
                        AccountTable.id
                )

            messagesWithSender
                .select(
                    ChatMessagesTable.id,
                    ChatMessagesTable.conversation,
                    ChatMessagesTable.senderAccount,
                    ChatMessagesTable.content,
                    ChatMessagesTable.createdAt,
                    ChatMessagesTable.readAt,
                    ChatMessagesTable.deletedAt,
                    ChatMessagesTable.deletedByAccount,
                    AccountTable.fullName
                )
                .where {
                    ChatMessagesTable.conversation eq
                            conversationId
                }
                .orderBy(
                    ChatMessagesTable.createdAt,
                    SortOrder.DESC
                )
                .limit(
                    normalizedLimit
                )
                .map { row ->
                    val senderAccountId =
                        row[
                            ChatMessagesTable.senderAccount
                        ].value

                    val deletedAt =
                        row[
                            ChatMessagesTable.deletedAt
                        ]

                    val deletedByAccountId =
                        row[
                            ChatMessagesTable.deletedByAccount
                        ]?.value

                    val isDeleted =
                        deletedAt != null

                    val safeContent =
                        if (isDeleted) {
                            ""
                        } else {
                            row[
                                ChatMessagesTable.content
                            ]
                        }

                    ChatMessageResponse(
                        id =
                            row[
                                ChatMessagesTable.id
                            ].value,

                        conversationId =
                            row[
                                ChatMessagesTable.conversation
                            ].value,

                        senderAccountId =
                            senderAccountId,

                        senderName =
                            row[
                                AccountTable.fullName
                            ],

                        content =
                            safeContent,

                        createdAt =
                            row[
                                ChatMessagesTable.createdAt
                            ].toString(),

                        readAt =
                            row[
                                ChatMessagesTable.readAt
                            ]?.toString(),

                        isMine =
                            senderAccountId ==
                                    authenticatedAccountId,

                        isDeleted =
                            isDeleted,

                        deletedAt =
                            deletedAt?.toString(),

                        deletedByAccountId =
                            deletedByAccountId
                    )
                }
                .reversed()
        }
    }






    fun markMessagesAsRead(
        tenantSchema: String,
        conversationId: Int,
        authenticatedAccountId: Int
    ): List<Int> {

        require(tenantSchema.isNotBlank()) {
            "Tenant schema is required."
        }

        require(conversationId > 0) {
            "A valid conversation ID is required."
        }

        require(authenticatedAccountId > 0) {
            "A valid authenticated account ID is required."
        }

        return transaction {
            setTenantSchema(
                tenantSchema
            )

            requireParticipantInCurrentTransaction(
                conversationId = conversationId,
                accountId = authenticatedAccountId
            )

            val unreadMessageIds =
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
                    .map { row ->
                        row[
                            ChatMessagesTable.id
                        ].value
                    }

            if (unreadMessageIds.isNotEmpty()) {
                val readAt =
                    LocalDateTime.now()

                ChatMessagesTable
                    .update({
                        (
                                ChatMessagesTable.conversation eq
                                        conversationId
                                ) and
                                (
                                        ChatMessagesTable.senderAccount neq
                                                authenticatedAccountId
                                        ) and
                                ChatMessagesTable.readAt.isNull()
                    }) { statement ->

                        statement[
                            ChatMessagesTable.readAt
                        ] = readAt
                    }
            }

            unreadMessageIds
        }
    }

    fun isParticipant(
        tenantSchema: String,
        conversationId: Int,
        accountId: Int
    ): Boolean {

        require(tenantSchema.isNotBlank()) {
            "Tenant schema is required."
        }

        if (
            conversationId <= 0 ||
            accountId <= 0
        ) {
            return false
        }

        return transaction {
            setTenantSchema(
                tenantSchema
            )

            isParticipantInCurrentTransaction(
                conversationId = conversationId,
                accountId = accountId
            )
        }
    }

    private fun findParticipantsInCurrentTransaction(
        conversationId: Int
    ): ConversationParticipants? {

        return ChatConversationsTable
            .select(
                ChatConversationsTable.id,
                ChatConversationsTable.student,
                ChatConversationsTable.parentAccount,
                ChatConversationsTable.teacherAccount,
                ChatConversationsTable.isClosed
            )
            .where {
                ChatConversationsTable.id eq
                        conversationId
            }
            .limit(1)
            .singleOrNull()
            ?.let { row ->

                ConversationParticipants(
                    conversationId =
                        row[
                            ChatConversationsTable.id
                        ].value,

                    studentId =
                        row[
                            ChatConversationsTable.student
                        ].value,

                    parentAccountId =
                        row[
                            ChatConversationsTable.parentAccount
                        ].value,

                    teacherAccountId =
                        row[
                            ChatConversationsTable.teacherAccount
                        ].value,

                    isClosed =
                        row[
                            ChatConversationsTable.isClosed
                        ]
                )
            }
    }

    private fun requireParticipantInCurrentTransaction(
        conversationId: Int,
        accountId: Int
    ) {
        require(
            isParticipantInCurrentTransaction(
                conversationId = conversationId,
                accountId = accountId
            )
        ) {
            "You are not a participant in this conversation."
        }
    }

    private fun isParticipantInCurrentTransaction(
        conversationId: Int,
        accountId: Int
    ): Boolean {

        return ChatConversationsTable
            .select(
                ChatConversationsTable.id
            )
            .where {
                (
                        ChatConversationsTable.id eq
                                conversationId
                        ) and
                        (
                                (
                                        ChatConversationsTable.parentAccount eq
                                                accountId
                                        ) or
                                        (
                                                ChatConversationsTable.teacherAccount eq
                                                        accountId
                                                )
                                )
            }
            .limit(1)
            .any()
    }
}