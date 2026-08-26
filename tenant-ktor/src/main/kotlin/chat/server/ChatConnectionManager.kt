package chat.server

import chat.models.ChatSocketEvent
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.sendSerialized

import java.util.concurrent.ConcurrentHashMap

object ChatConnectionManager {



    suspend fun sendToConversationParticipants(
        parentAccountId: Int,
        teacherAccountId: Int,
        event: ChatSocketEvent
    ) {
        val participantAccountIds =
            setOf(
                parentAccountId,
                teacherAccountId
            )

        participantAccountIds.forEach { accountId ->
            sendToAccount(
                accountId = accountId,
                event = event
            )
        }
    }

    fun isConnected(
        accountId: Int
    ): Boolean {
        if (accountId <= 0) {
            return false
        }

        return sessions[accountId]
            ?.isNotEmpty() == true
    }


    fun getSessionCount(
        accountId: Int
    ): Int {

        if (accountId <= 0) {
            return 0
        }

        return sessions[accountId]
            ?.size
            ?: 0
    }


    private val sessions =
        ConcurrentHashMap<
                Int,
                MutableSet<DefaultWebSocketServerSession>
                >()

    fun connect(
        accountId: Int,
        session: DefaultWebSocketServerSession
    ) {
        require(accountId > 0) {
            "A valid account ID is required."
        }

        sessions
            .computeIfAbsent(accountId) {
                ConcurrentHashMap.newKeySet<
                        DefaultWebSocketServerSession
                        >()
            }
            .add(session)

        println(
            "[ChatConnectionManager] " +
                    "Connected accountId=$accountId, " +
                    "activeSessions=${getSessionCount(accountId)}"
        )
    }

    fun disconnect(
        accountId: Int,
        session: DefaultWebSocketServerSession
    ) {
        val accountSessions =
            sessions[accountId]
                ?: return

        accountSessions.remove(session)

        if (accountSessions.isEmpty()) {
            sessions.remove(
                accountId,
                accountSessions
            )
        }

        println(
            "[ChatConnectionManager] " +
                    "Disconnected accountId=$accountId, " +
                    "activeSessions=${getSessionCount(accountId)}"
        )
    }

    suspend fun sendToAccount(
        accountId: Int,
        event: ChatSocketEvent
    ): Int {
        if (accountId <= 0) {
            return 0
        }

        val accountSessions =
            sessions[accountId]
                ?.toList()
                .orEmpty()

        if (accountSessions.isEmpty()) {
            return 0
        }

        val failedSessions =
            mutableListOf<
                    DefaultWebSocketServerSession
                    >()

        var successfulDeliveries = 0

        accountSessions.forEach { session ->
            try {
                session.sendSerialized(
                    event
                )

                successfulDeliveries += 1
            } catch (exception: Exception) {
                println(
                    "[ChatConnectionManager] " +
                            "Delivery failed for accountId=$accountId: " +
                            exception.message
                )

                failedSessions.add(
                    session
                )
            }
        }

        failedSessions.forEach { failedSession ->
            disconnect(
                accountId = accountId,
                session = failedSession
            )
        }

        return successfulDeliveries
    }

    suspend fun sendToAccounts(
        accountIds: Collection<Int>,
        event: ChatSocketEvent
    ): Int {

        val normalizedAccountIds =
            accountIds
                .filter { accountId ->
                    accountId > 0
                }
                .distinct()

        if (normalizedAccountIds.isEmpty()) {
            return 0
        }

        var successfulDeliveries = 0

        normalizedAccountIds.forEach { accountId ->

            successfulDeliveries +=
                sendToAccount(
                    accountId = accountId,
                    event = event
                )
        }

        return successfulDeliveries
    }}