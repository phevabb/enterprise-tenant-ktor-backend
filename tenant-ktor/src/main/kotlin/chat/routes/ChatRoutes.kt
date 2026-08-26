package chat.routes

import chat.models.CreateParentTeacherConversationRequest
import chat.models.DeleteChatMessageResponse
import chat.repositories.ChatConversationRepository
import chat.repositories.ChatRepository
import chat.services.ChatService
import com.example.tenant.currentTenant
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.log
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.websocket.webSocket
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlin.compareTo

@Serializable
data class ChatRouteResponse(
    val success: Boolean,
    val message: String
)

fun Route.chatRoutes() {

    authenticate("auth-jwt") {

        route("/chat") {



            delete("/messages/{messageId}") {
                var diagnosticAccountId: Int? = null
                var diagnosticMessageId: Int? = null
                var diagnosticTenantSchema: String? = null

                try {
                    val tenant =
                        call.currentTenant()

                    diagnosticTenantSchema =
                        tenant.tenantSchema

                    val principal =
                        call.principal<JWTPrincipal>()
                            ?: return@delete call.respond(
                                status =
                                    HttpStatusCode.Unauthorized,

                                message =
                                    ChatRouteResponse(
                                        success = false,

                                        message =
                                            "Authentication required."
                                    )
                            )

                    val authenticatedAccountId =
                        principal.payload
                            .getClaim("userId")
                            .asInt()
                            ?.takeIf { accountId ->
                                accountId > 0
                            }
                            ?: return@delete call.respond(
                                status =
                                    HttpStatusCode.Unauthorized,

                                message =
                                    ChatRouteResponse(
                                        success = false,

                                        message =
                                            "A valid authenticated account ID is required."
                                    )
                            )

                    diagnosticAccountId =
                        authenticatedAccountId

                    val messageId =
                        call.parameters["messageId"]
                            ?.toIntOrNull()
                            ?.takeIf { id ->
                                id > 0
                            }
                            ?: return@delete call.respond(
                                status =
                                    HttpStatusCode.BadRequest,

                                message =
                                    ChatRouteResponse(
                                        success = false,

                                        message =
                                            "A valid message ID is required."
                                    )
                            )

                    diagnosticMessageId =
                        messageId

                    println(
                        "[ChatRoutes] Deleting message: " +
                                "accountId=$authenticatedAccountId, " +
                                "messageId=$messageId, " +
                                "tenantSchema=${tenant.tenantSchema}"
                    )

                    val result =
                        ChatService.deleteMessage(
                            tenantSchema =
                                tenant.tenantSchema,

                            authenticatedAccountId =
                                authenticatedAccountId,

                            messageId =
                                messageId
                        )

                    println(
                        "[ChatRoutes] Message deleted successfully: " +
                                "accountId=$authenticatedAccountId, " +
                                "messageId=${result.messageId}, " +
                                "conversationId=${result.conversationId}, " +
                                "deletedAt=${result.deletedAt}"
                    )

                    call.respond(
                        status =
                            HttpStatusCode.OK,

                        message =
                            DeleteChatMessageResponse(
                                success = true,

                                message =
                                    "Message deleted.",

                                messageId =
                                    result.messageId,

                                conversationId =
                                    result.conversationId,

                                deletedAt =
                                    result.deletedAt
                            )
                    )
                } catch (
                    exception: IllegalArgumentException
                ) {
                    println(
                        "[ChatRoutes] Message deletion rejected: " +
                                "accountId=$diagnosticAccountId, " +
                                "messageId=$diagnosticMessageId, " +
                                "tenantSchema=$diagnosticTenantSchema, " +
                                "reason=${exception.message}"
                    )

                    call.respond(
                        status =
                            HttpStatusCode.BadRequest,

                        message =
                            ChatRouteResponse(
                                success = false,

                                message =
                                    exception.message
                                        ?: "Unable to delete the message."
                            )
                    )
                } catch (exception: Exception) {
                    println(
                        "[ChatRoutes] Message deletion failed: " +
                                "accountId=$diagnosticAccountId, " +
                                "messageId=$diagnosticMessageId, " +
                                "tenantSchema=$diagnosticTenantSchema, " +
                                "exceptionType=" +
                                "${exception::class.qualifiedName}, " +
                                "reason=${exception.message}"
                    )

                    exception.printStackTrace()

                    call.respond(
                        status =
                            HttpStatusCode.InternalServerError,

                        message =
                            ChatRouteResponse(
                                success = false,

                                message =
                                    "Unable to delete the message."
                            )
                    )
                }
            }



            get("/conversations") {
                try {
                    val tenant =
                        call.currentTenant()

                    val principal =
                        call.principal<JWTPrincipal>()
                            ?: return@get call.respond(
                                status =
                                    HttpStatusCode.Unauthorized,

                                message =
                                    ChatRouteResponse(
                                        success = false,
                                        message =
                                            "Authentication required."
                                    )
                            )

                    val authenticatedAccountId =
                        principal.payload
                            .getClaim("userId")
                            .asInt()
                            ?.takeIf { accountId ->
                                accountId > 0
                            }
                            ?: return@get call.respond(
                                status =
                                    HttpStatusCode.Unauthorized,

                                message =
                                    ChatRouteResponse(
                                        success = false,
                                        message =
                                            "A valid authenticated account ID is required."
                                    )
                            )

                    val conversations =
                        ChatConversationRepository
                            .findConversationsForAccount(
                                tenantSchema =
                                    tenant.tenantSchema,

                                authenticatedAccountId =
                                    authenticatedAccountId
                            )

                    call.respond(
                        status =
                            HttpStatusCode.OK,

                        message =
                            conversations
                    )
                } catch (
                    exception: IllegalArgumentException
                ) {
                    call.respond(
                        status =
                            HttpStatusCode.BadRequest,

                        message =
                            ChatRouteResponse(
                                success = false,
                                message =
                                    exception.message
                                        ?: "Unable to retrieve conversations."
                            )
                    )
                } catch (exception: Exception) {
                    println(
                        "[ChatRoutes] " +
                                "Failed to retrieve conversations: " +
                                exception.message
                    )

                    exception.printStackTrace()

                    call.respond(
                        status =
                            HttpStatusCode.InternalServerError,

                        message =
                            ChatRouteResponse(
                                success = false,
                                message =
                                    "Unable to retrieve conversations."
                            )
                    )
                }
            }


            post("/conversations") {
                /*
                 * These values are declared outside the try block so they remain
                 * available when an exception is caught.
                 */
                var diagnosticAccountId: Int? = null
                var diagnosticStudentId: Int? = null
                var diagnosticTenantSchema: String? = null
                var diagnosticRole: String? = null

                try {
                    val tenant =
                        call.currentTenant()

                    diagnosticTenantSchema =
                        tenant.tenantSchema

                    val principal =
                        call.principal<JWTPrincipal>()
                            ?: return@post call.respond(
                                status =
                                    HttpStatusCode.Unauthorized,

                                message =
                                    ChatRouteResponse(
                                        success = false,

                                        message =
                                            "Authentication required."
                                    )
                            )

                    val authenticatedAccountId =
                        principal.payload
                            .getClaim("userId")
                            .asInt()
                            ?.takeIf { accountId ->
                                accountId > 0
                            }
                            ?: return@post call.respond(
                                status =
                                    HttpStatusCode.Unauthorized,

                                message =
                                    ChatRouteResponse(
                                        success = false,

                                        message =
                                            "A valid authenticated account ID is required."
                                    )
                            )

                    val authenticatedRole =
                        principal.payload
                            .getClaim("role")
                            .asString()
                            ?.trim()
                            ?.lowercase()
                            .orEmpty()

                    diagnosticAccountId =
                        authenticatedAccountId

                    diagnosticRole =
                        authenticatedRole

                    /*
                     * Your teacher JWT uses role = staff.
                     *
                     * Support both values in case another teacher account uses
                     * role = teacher.
                     */


                    if (

                        authenticatedRole != "staff" &&

                    authenticatedRole != "student"

                    ) {
                        println(
                            "[ChatRoutes] Conversation creation forbidden: " +
                                    "accountId=$authenticatedAccountId, " +
                                    "role=$authenticatedRole, " +
                                    "tenantSchema=${tenant.tenantSchema}"
                        )

                        return@post call.respond(
                            status =
                                HttpStatusCode.Forbidden,

                            message =
                                ChatRouteResponse(
                                    success = false,

                                    message =
                                        "Only an authenticated staff member can " +
                                                "start a parent conversation."
                                )
                        )
                    }

                    val request =
                        call.receive<
                                CreateParentTeacherConversationRequest
                                >()

                    diagnosticStudentId =
                        request.studentId

                    if (request.studentId <= 0) {
                        return@post call.respond(
                            status =
                                HttpStatusCode.BadRequest,

                            message =
                                ChatRouteResponse(
                                    success = false,

                                    message =
                                        "A valid student ID is required."
                                )
                        )
                    }

                    println(
                        "[ChatRoutes] Creating parent-teacher conversation: " +
                                "accountId=$authenticatedAccountId, " +
                                "role=$authenticatedRole, " +
                                "studentId=${request.studentId}, " +
                                "tenantSchema=${tenant.tenantSchema}"
                    )

                    val conversation =
                        ChatConversationRepository
                            .findOrCreateParentTeacherConversation(
                                tenantSchema =
                                    tenant.tenantSchema,

                                authenticatedAccountId =
                                    authenticatedAccountId,

                                studentId =
                                    request.studentId
                            )

                    println(
                        "[ChatRoutes] Parent-teacher conversation opened: " +
                                "accountId=$authenticatedAccountId, " +
                                "studentId=${request.studentId}, " +
                                "tenantSchema=${tenant.tenantSchema}"
                    )

                    call.respond(
                        status =
                            HttpStatusCode.OK,

                        message =
                            conversation
                    )
                } catch (
                    exception: SerializationException
                ) {
                    /*
                     * SerializationException must come before
                     * IllegalArgumentException because it inherits from it.
                     */
                    println(
                        "[ChatRoutes] Invalid conversation request body: " +
                                "accountId=$diagnosticAccountId, " +
                                "role=$diagnosticRole, " +
                                "studentId=$diagnosticStudentId, " +
                                "tenantSchema=$diagnosticTenantSchema, " +
                                "reason=${exception.message}"
                    )

                    exception.printStackTrace()

                    call.respond(
                        status =
                            HttpStatusCode.BadRequest,

                        message =
                            ChatRouteResponse(
                                success = false,

                                message =
                                    "The conversation request body is invalid."
                            )
                    )
                } catch (
                    exception: IllegalArgumentException
                ) {
                    /*
                     * Repository business-rule failures are returned here.
                     *
                     * Examples:
                     * - Student was not found
                     * - Staff is not assigned to the student's class
                     * - Student has no linked parent account
                     */
                    println(
                        "[ChatRoutes] Conversation creation rejected: " +
                                "accountId=$diagnosticAccountId, " +
                                "role=$diagnosticRole, " +
                                "studentId=$diagnosticStudentId, " +
                                "tenantSchema=$diagnosticTenantSchema, " +
                                "reason=${exception.message}"
                    )

                    call.respond(
                        status =
                            HttpStatusCode.BadRequest,

                        message =
                            ChatRouteResponse(
                                success = false,

                                message =
                                    exception.message
                                        ?: "Unable to open the conversation."
                            )
                    )
                } catch (exception: Exception) {
                    println(
                        "[ChatRoutes] Failed to create conversation: " +
                                "accountId=$diagnosticAccountId, " +
                                "role=$diagnosticRole, " +
                                "studentId=$diagnosticStudentId, " +
                                "tenantSchema=$diagnosticTenantSchema, " +
                                "exceptionType=${exception::class.qualifiedName}, " +
                                "reason=${exception.message}"
                    )

                    exception.printStackTrace()

                    call.respond(
                        status =
                            HttpStatusCode.InternalServerError,

                        message =
                            ChatRouteResponse(
                                success = false,

                                message =
                                    "Unable to open the parent-teacher conversation."
                            )
                    )
                }
            }










            /*
             * GET /chat/conversations/{conversationId}/messages
             *
             * Optional query parameter:
             * ?limit=50
             */
            /*
  * GET /chat/conversations/{conversationId}/messages
  *
  * Optional query parameter:
  * ?limit=50
  */
            get(
                "/conversations/{conversationId}/messages"
            ) {
                /*
                 * Diagnostic values remain available inside catch blocks.
                 */
                var diagnosticAccountId: Int? = null
                var diagnosticConversationId: Int? = null
                var diagnosticTenantSchema: String? = null

                try {
                    /*
                     * Resolve the tenant for the current request.
                     */
                    val tenant =
                        call.currentTenant()

                    diagnosticTenantSchema =
                        tenant.tenantSchema

                    /*
                     * Retrieve the authenticated JWT principal.
                     */
                    val principal =
                        call.principal<JWTPrincipal>()
                            ?: return@get call.respond(
                                status =
                                    HttpStatusCode.Unauthorized,

                                message =
                                    ChatRouteResponse(
                                        success = false,

                                        message =
                                            "Authentication required."
                                    )
                            )

                    /*
                     * The userId JWT claim represents AccountTable.id.
                     */
                    val authenticatedAccountId =
                        principal.payload
                            .getClaim("userId")
                            .asInt()
                            ?.takeIf { accountId ->
                                accountId > 0
                            }
                            ?: return@get call.respond(
                                status =
                                    HttpStatusCode.Unauthorized,

                                message =
                                    ChatRouteResponse(
                                        success = false,

                                        message =
                                            "A valid authenticated account ID is required."
                                    )
                            )

                    diagnosticAccountId =
                        authenticatedAccountId

                    /*
                     * Read and validate the conversation ID.
                     */
                    val conversationId =
                        call.parameters[
                            "conversationId"
                        ]
                            ?.trim()
                            ?.toIntOrNull()
                            ?.takeIf { id ->
                                id > 0
                            }
                            ?: return@get call.respond(
                                status =
                                    HttpStatusCode.BadRequest,

                                message =
                                    ChatRouteResponse(
                                        success = false,

                                        message =
                                            "A valid conversation ID is required."
                                    )
                            )

                    diagnosticConversationId =
                        conversationId

                    /*
                     * Read and normalize the optional message limit.
                     */
                    val limit =
                        call.request
                            .queryParameters["limit"]
                            ?.trim()
                            ?.toIntOrNull()
                            ?.coerceIn(
                                minimumValue = 1,
                                maximumValue = 100
                            )
                            ?: 50

                    println(
                        "[ChatRoutes] Retrieving messages: " +
                                "accountId=$authenticatedAccountId, " +
                                "conversationId=$conversationId, " +
                                "limit=$limit, " +
                                "tenantSchema=${tenant.tenantSchema}"
                    )

                    /*
                     * The repository verifies that the authenticated account is
                     * either the parent-side account or the assigned staff account.
                     */
                    val messages =
                        ChatRepository.findMessages(
                            tenantSchema =
                                tenant.tenantSchema,

                            conversationId =
                                conversationId,

                            authenticatedAccountId =
                                authenticatedAccountId,

                            limit =
                                limit
                        )

                    println(
                        "[ChatRoutes] Messages retrieved successfully: " +
                                "accountId=$authenticatedAccountId, " +
                                "conversationId=$conversationId, " +
                                "messageCount=${messages.size}, " +
                                "tenantSchema=${tenant.tenantSchema}"
                    )

                    call.respond(
                        status =
                            HttpStatusCode.OK,

                        message =
                            messages
                    )
                } catch (
                    exception: IllegalArgumentException
                ) {
                    /*
                     * This normally represents an authorization or business-rule
                     * failure from ChatRepository.findMessages().
                     */
                    println(
                        "[ChatRoutes] Message retrieval rejected: " +
                                "accountId=$diagnosticAccountId, " +
                                "conversationId=$diagnosticConversationId, " +
                                "tenantSchema=$diagnosticTenantSchema, " +
                                "reason=${exception.message}"
                    )

                    call.respond(
                        status =
                            HttpStatusCode.Forbidden,

                        message =
                            ChatRouteResponse(
                                success = false,

                                message =
                                    exception.message
                                        ?: "You cannot access this conversation."
                            )
                    )
                } catch (exception: Exception) {
                    /*
                     * Unexpected database, Exposed, or serialization failure.
                     */
                    println(
                        "[ChatRoutes] Failed to retrieve messages: " +
                                "accountId=$diagnosticAccountId, " +
                                "conversationId=$diagnosticConversationId, " +
                                "tenantSchema=$diagnosticTenantSchema, " +
                                "exceptionType=${exception::class.qualifiedName}, " +
                                "reason=${exception.message}"
                    )

                    exception.printStackTrace()

                    call.respond(
                        status =
                            HttpStatusCode.InternalServerError,

                        message =
                            ChatRouteResponse(
                                success = false,

                                message =
                                    "Unable to retrieve chat messages."
                            )
                    )
                }
            }
            /*
             * PATCH /chat/conversations/{conversationId}/read
             */





























            patch(
                "/conversations/{conversationId}/read"
            ) {
                try {
                    val tenant =
                        call.currentTenant()

                    val principal =
                        call.principal<JWTPrincipal>()
                            ?: return@patch call.respond(
                                status =
                                    HttpStatusCode.Unauthorized,

                                message =
                                    ChatRouteResponse(
                                        success = false,
                                        message =
                                            "Authentication required."
                                    )
                            )

                    val accountId =
                        principal.payload
                            .getClaim("userId")
                            .asInt()
                            ?.takeIf { id ->
                                id > 0
                            }
                            ?: return@patch call.respond(
                                status =
                                    HttpStatusCode.Unauthorized,

                                message =
                                    ChatRouteResponse(
                                        success = false,
                                        message =
                                            "A valid authenticated account ID is required."
                                    )
                            )

                    val conversationId =
                        call.parameters[
                            "conversationId"
                        ]
                            ?.toIntOrNull()
                            ?.takeIf { id ->
                                id > 0
                            }
                            ?: return@patch call.respond(
                                status =
                                    HttpStatusCode.BadRequest,

                                message =
                                    ChatRouteResponse(
                                        success = false,
                                        message =
                                            "A valid conversation ID is required."
                                    )
                            )

                    ChatService.markConversationAsRead(
                        tenantSchema =
                            tenant.tenantSchema,

                        authenticatedAccountId =
                            accountId,

                        conversationId =
                            conversationId
                    )

                    call.respond(
                        status = HttpStatusCode.OK,

                        message =
                            ChatRouteResponse(
                                success = true,
                                message =
                                    "Conversation marked as read."
                            )
                    )

                } catch (
                    exception: IllegalArgumentException
                ) {
                    call.respond(
                        status = HttpStatusCode.Forbidden,

                        message =
                            ChatRouteResponse(
                                success = false,
                                message =
                                    exception.message
                                        ?: "You cannot access this conversation."
                            )
                    )

                } catch (exception: Exception) {
                    println(
                        "[ChatRoutes] " +
                                "Failed to mark messages as read: " +
                                exception.message
                    )

                    exception.printStackTrace()

                    call.respond(
                        status =
                            HttpStatusCode.InternalServerError,

                        message =
                            ChatRouteResponse(
                                success = false,
                                message =
                                    "Unable to mark the conversation as read."
                            )
                    )
                }
            }
        }
    }
}