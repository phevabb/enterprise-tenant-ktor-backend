package chat.routes

import chat.models.ChatEventType
import chat.models.ChatSocketEvent
import chat.server.ChatConnectionManager
import chat.services.ChatService
import com.example.tenant.currentTenant
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.receiveDeserialized
import io.ktor.server.websocket.sendSerialized
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close
import kotlinx.coroutines.CancellationException

fun Route.chatSocketRoutes() {
    authenticate("auth-jwt") {
        webSocket("/chat/ws") {
            /*
             * Authentication has already been performed by auth-jwt.
             */
            val principal =
                call.principal<JWTPrincipal>()

            println(
                "[ChatSocket] Authorization header present=" +
                        !call.request.headers["Authorization"]
                            .isNullOrBlank()
            )

            println(
                "[ChatSocket] access_token query present=" +
                        !call.request
                            .queryParameters["access_token"]
                            .isNullOrBlank()
            )

            println(
                "[ChatSocket] token query present=" +
                        !call.request
                            .queryParameters["token"]
                            .isNullOrBlank()
            )

            println(
                "[ChatSocket] JWT principal available=" +
                        (principal != null)
            )

            if (principal == null) {
                close(
                    CloseReason(
                        code =
                            CloseReason.Codes.VIOLATED_POLICY,

                        message =
                            "Authentication required."
                    )
                )

                return@webSocket
            }

            val accountId =
                principal.payload
                    .getClaim("userId")
                    .asInt()
                    ?.takeIf { id ->
                        id > 0
                    }

            if (accountId == null) {
                close(
                    CloseReason(
                        code =
                            CloseReason.Codes.VIOLATED_POLICY,

                        message =
                            "Valid userId claim is required."
                    )
                )

                return@webSocket
            }

            val tenant =
                try {
                    call.currentTenant()
                } catch (exception: Exception) {
                    println(
                        "[ChatSocket] Tenant resolution failed: " +
                                "accountId=$accountId, " +
                                "reason=${exception.message}"
                    )

                    close(
                        CloseReason(
                            code =
                                CloseReason.Codes.VIOLATED_POLICY,

                            message =
                                exception.message
                                    ?: "A valid tenant is required."
                        )
                    )

                    return@webSocket
                }

            val tenantSchema =
                tenant.tenantSchema

            /*
             * IMPORTANT:
             *
             * Do not call TenantSchemaService.ensureChatTablesForTenant()
             * inside this WebSocket route.
             *
             * A schema migration must not run whenever a user connects.
             * Concurrent socket connections can attempt to add the same
             * database column and cause PostgreSQL duplicate-column errors.
             */

            ChatConnectionManager.connect(
                accountId =
                    accountId,

                session =
                    this
            )

            println(
                "[ChatSocket] Connected: " +
                        "accountId=$accountId, " +
                        "tenantSchema=$tenantSchema"
            )

            try {
                sendSerialized(
                    ChatSocketEvent(
                        type =
                            ChatEventType.CONNECTED,

                        content =
                            "Chat connection established."
                    )
                )

                while (true) {
                    val event =
                        receiveDeserialized<
                                ChatSocketEvent
                                >()

                    try {
                        val normalizedEventType =
                            event.type
                                .trim()
                                .uppercase()

                        println(
                            "[ChatSocket] Event received: " +
                                    "accountId=$accountId, " +
                                    "eventType=$normalizedEventType, " +
                                    "conversationId=${event.conversationId}"
                        )

                        when (normalizedEventType) {
                            ChatEventType.SEND_MESSAGE -> {
                                handleSendMessageEvent(
                                    tenantSchema =
                                        tenantSchema,

                                    accountId =
                                        accountId,

                                    event =
                                        event
                                )
                            }

                            ChatEventType.MESSAGE_READ -> {
                                handleMessageReadEvent(
                                    tenantSchema =
                                        tenantSchema,

                                    accountId =
                                        accountId,

                                    event =
                                        event
                                )
                            }

                            else -> {
                                sendSerialized(
                                    ChatSocketEvent(
                                        type =
                                            ChatEventType.ERROR,

                                        conversationId =
                                            event.conversationId,

                                        errorMessage =
                                            "Unsupported chat event type."
                                    )
                                )
                            }
                        }
                    } catch (
                        exception: IllegalArgumentException
                    ) {
                        println(
                            "[ChatSocket] Event rejected: " +
                                    "accountId=$accountId, " +
                                    "eventType=${event.type}, " +
                                    "conversationId=${event.conversationId}, " +
                                    "reason=${exception.message}"
                        )

                        sendSerialized(
                            ChatSocketEvent(
                                type =
                                    ChatEventType.ERROR,

                                conversationId =
                                    event.conversationId,

                                errorMessage =
                                    exception.message
                                        ?: "Invalid chat request."
                            )
                        )
                    } catch (
                        exception: CancellationException
                    ) {
                        throw exception
                    } catch (exception: Exception) {
                        println(
                            "[ChatSocket] Event processing failed: " +
                                    "accountId=$accountId, " +
                                    "tenantSchema=$tenantSchema, " +
                                    "eventType=${event.type}, " +
                                    "conversationId=${event.conversationId}, " +
                                    "exceptionType=" +
                                    "${exception::class.qualifiedName}, " +
                                    "reason=${exception.message}"
                        )

                        exception.printStackTrace()

                        sendSerialized(
                            ChatSocketEvent(
                                type =
                                    ChatEventType.ERROR,

                                conversationId =
                                    event.conversationId,

                                errorMessage =
                                    exception.message
                                        ?: "Unable to process the chat request."
                            )
                        )
                    }
                }
            } catch (
                exception: CancellationException
            ) {
                println(
                    "[ChatSocket] Connection cancelled: " +
                            "accountId=$accountId, " +
                            "tenantSchema=$tenantSchema"
                )

                throw exception
            } catch (exception: Exception) {
                println(
                    "[ChatSocket] Account disconnected: " +
                            "accountId=$accountId, " +
                            "tenantSchema=$tenantSchema, " +
                            "reason=${exception.message}"
                )
            } finally {
                ChatConnectionManager.disconnect(
                    accountId =
                        accountId,

                    session =
                        this
                )

                println(
                    "[ChatSocket] Connection removed: " +
                            "accountId=$accountId, " +
                            "tenantSchema=$tenantSchema"
                )
            }
        }
    }
}

/*
|--------------------------------------------------------------------------
| Send message
|--------------------------------------------------------------------------
*/

private suspend fun DefaultWebSocketServerSession
        .handleSendMessageEvent(
    tenantSchema: String,
    accountId: Int,
    event: ChatSocketEvent
) {

    val conversationId =
        event.conversationId
            ?.takeIf { id ->
                id > 0
            }

    if (conversationId == null) {
        sendSerialized(
            ChatSocketEvent(
                type =
                    ChatEventType.ERROR,

                errorMessage =
                    "A valid conversation ID is required."
            )
        )

        return
    }

    val content =
        event.content
            ?.trim()
            .orEmpty()

    if (content.isBlank()) {
        sendSerialized(
            ChatSocketEvent(
                type =
                    ChatEventType.ERROR,

                conversationId =
                    conversationId,

                errorMessage =
                    "Message cannot be empty."
            )
        )

        return
    }

    if (content.length > 2000) {
        sendSerialized(
            ChatSocketEvent(
                type =
                    ChatEventType.ERROR,

                conversationId =
                    conversationId,

                errorMessage =
                    "Message cannot exceed 2,000 characters."
            )
        )

        return
    }

    ChatService.sendMessage(
        tenantSchema =
            tenantSchema,

        authenticatedAccountId =
            accountId,

        conversationId =
            conversationId,

        content =
            content
    )
}

/*
|--------------------------------------------------------------------------
| Mark conversation as read
|--------------------------------------------------------------------------
*/

private suspend fun DefaultWebSocketServerSession
        .handleMessageReadEvent(
    tenantSchema: String,
    accountId: Int,
    event: ChatSocketEvent
) {

    val conversationId =
        event.conversationId
            ?.takeIf { id ->
                id > 0
            }

    if (conversationId == null) {
        sendSerialized(
            ChatSocketEvent(
                type =
                    ChatEventType.ERROR,

                errorMessage =
                    "A valid conversation ID is required."
            )
        )

        return
    }

    ChatService.markConversationAsRead(
        tenantSchema =
            tenantSchema,

        authenticatedAccountId =
            accountId,

        conversationId =
            conversationId
    )
}