package chat.services

import chat.models.ChatEventType
import chat.models.ChatMessageResponse
import chat.models.ChatSocketEvent
import chat.models.DeleteChatMessageResult
import chat.repositories.ChatRepository
import chat.server.ChatConnectionManager
import java.time.LocalDateTime

object ChatService {



    suspend fun deleteMessage(
        tenantSchema: String,
        authenticatedAccountId: Int,
        messageId: Int
    ): DeleteChatMessageResult {

        val result =
            ChatRepository.deleteMessage(
                tenantSchema =
                    tenantSchema,

                authenticatedAccountId =
                    authenticatedAccountId,

                messageId =
                    messageId
            )

        val event =
            ChatSocketEvent(
                type =
                    ChatEventType.MESSAGE_DELETED,

                messageId =
                    result.messageId,

                conversationId =
                    result.conversationId,

                deletedAt =
                    result.deletedAt,

                deletedByAccountId =
                    result.deletedByAccountId
            )

        ChatConnectionManager
            .sendToConversationParticipants(
                parentAccountId =
                    result.parentAccountId,

                teacherAccountId =
                    result.teacherAccountId,

                event =
                    event
            )

        println(
            "[ChatService] Message deletion broadcast: " +
                    "messageId=${result.messageId}, " +
                    "conversationId=${result.conversationId}, " +
                    "deletedBy=${result.deletedByAccountId}"
        )

        return result
    }




    suspend fun sendMessage(
        tenantSchema: String,
        authenticatedAccountId: Int,
        conversationId: Int,
        content: String
    ): ChatMessageResponse {

        require(tenantSchema.isNotBlank()) {
            "Tenant schema is required."
        }

        require(authenticatedAccountId > 0) {
            "A valid authenticated account ID is required."
        }

        require(conversationId > 0) {
            "A valid conversation ID is required."
        }

        val normalizedContent =
            content.trim()

        require(normalizedContent.isNotBlank()) {
            "Message cannot be empty."
        }

        require(normalizedContent.length <= 2000) {
            "Message cannot exceed 2,000 characters."
        }

        /*
         * ChatRepository.createMessage() confirms that the
         * authenticated user belongs to this conversation.
         *
         * It saves the message before it is sent through WebSocket.
         */
        val savedMessage =
            ChatRepository.createMessage(
                tenantSchema = tenantSchema,
                conversationId = conversationId,
                senderAccountId =
                    authenticatedAccountId,
                content = normalizedContent
            )

        val participants =
            ChatRepository.findParticipants(
                tenantSchema = tenantSchema,
                conversationId = conversationId
            )
                ?: throw IllegalArgumentException(
                    "Conversation was not found."
                )

        require(!participants.isClosed) {
            "This conversation is closed."
        }

        val recipientAccountId =
            when (authenticatedAccountId) {
                participants.parentAccountId -> {
                    participants.teacherAccountId
                }

                participants.teacherAccountId -> {
                    participants.parentAccountId
                }

                else -> {
                    throw IllegalArgumentException(
                        "You are not a participant in this conversation."
                    )
                }
            }

        val socketEvent =
            ChatSocketEvent(
                type = ChatEventType.NEW_MESSAGE,
                conversationId =
                    savedMessage.conversationId,
                messageId =
                    savedMessage.id,
                senderAccountId =
                    savedMessage.senderAccountId,
                senderName =
                    savedMessage.senderName,
                content =
                    savedMessage.content,
                sentAt =
                    savedMessage.createdAt
            )

        /*
         * Deliver the message to every active session belonging
         * to the recipient.
         */
        val recipientDeliveries =
            ChatConnectionManager.sendToAccount(
                accountId = recipientAccountId,
                event = socketEvent
            )

        /*
         * Send the saved message back to every active session
         * belonging to the sender.
         *
         * This synchronizes multiple browser tabs and devices.
         */
        val senderDeliveries =
            ChatConnectionManager.sendToAccount(
                accountId = authenticatedAccountId,
                event = socketEvent
            )

        println(
            "[ChatService] Message saved and delivered: " +
                    "messageId=${savedMessage.id}, " +
                    "conversationId=$conversationId, " +
                    "senderAccountId=$authenticatedAccountId, " +
                    "recipientAccountId=$recipientAccountId, " +
                    "senderDeliveries=$senderDeliveries, " +
                    "recipientDeliveries=$recipientDeliveries"
        )

        return savedMessage
    }

    suspend fun markConversationAsRead(
        tenantSchema: String,
        authenticatedAccountId: Int,
        conversationId: Int
    ) {
        require(tenantSchema.isNotBlank()) {
            "Tenant schema is required."
        }

        require(authenticatedAccountId > 0) {
            "A valid authenticated account ID is required."
        }

        require(conversationId > 0) {
            "A valid conversation ID is required."
        }

        val participants =
            ChatRepository.findParticipants(
                tenantSchema = tenantSchema,
                conversationId = conversationId
            )
                ?: throw IllegalArgumentException(
                    "Conversation was not found."
                )

        require(!participants.isClosed) {
            "This conversation is closed."
        }

        val otherAccountId =
            when (authenticatedAccountId) {
                participants.parentAccountId -> {
                    participants.teacherAccountId
                }

                participants.teacherAccountId -> {
                    participants.parentAccountId
                }

                else -> {
                    throw IllegalArgumentException(
                        "You are not a participant in this conversation."
                    )
                }
            }

        val readMessageIds =
            ChatRepository.markMessagesAsRead(
                tenantSchema = tenantSchema,
                conversationId = conversationId,
                authenticatedAccountId = authenticatedAccountId
            )

        if (readMessageIds.isEmpty()) {
            println(
                "[ChatService] No unread messages found: " +
                        "conversationId=$conversationId, " +
                        "accountId=$authenticatedAccountId"
            )

            return
        }

        val readAt =
            LocalDateTime.now()
                .toString()

        val readEvent =
            ChatSocketEvent(
                type = ChatEventType.MESSAGE_READ,
                conversationId = conversationId,
                readAt = readAt
            )

        /*
         * Notify the other participant that their messages
         * have been read.
         */
        val otherAccountDeliveries =
            ChatConnectionManager.sendToAccount(
                accountId = otherAccountId,
                event = readEvent
            )

        /*
         * Update any other browser tabs or devices belonging
         * to the account that read the messages.
         */
        val readerDeliveries =
            ChatConnectionManager.sendToAccount(
                accountId = authenticatedAccountId,
                event = readEvent
            )

        println(
            "[ChatService] Conversation marked as read: " +
                    "conversationId=$conversationId, " +
                    "readerAccountId=$authenticatedAccountId, " +
                    "otherAccountId=$otherAccountId, " +
                    "readMessageCount=${readMessageIds.size}, " +
                    "readerDeliveries=$readerDeliveries, " +
                    "otherAccountDeliveries=$otherAccountDeliveries"
        )
    }}






























