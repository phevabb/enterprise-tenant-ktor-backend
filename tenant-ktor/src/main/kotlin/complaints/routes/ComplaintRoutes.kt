package complaints.routes



import complaints.models.AssignComplaintRequest
import complaints.models.ComplaintActionResponse
import complaints.models.ComplaintReplyActionResponse
import complaints.models.CreateAdminComplaintReplyRequest
import complaints.models.CreateComplaintReplyRequest
import complaints.models.CreateParentComplaintRequest
import complaints.models.UpdateComplaintStatusRequest
import complaints.repositories.ComplaintRepository
import com.example.tenant.currentTenant
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.complaintRoutes() {
    authenticate("auth-jwt") {
        route("/parent/complaints") {
            post {
                val principal =
                    call.principal<JWTPrincipal>()
                        ?: return@post call.respond(
                            HttpStatusCode.Unauthorized,
                            ComplaintActionResponse(
                                success = false,
                                message = "Authentication required."
                            )
                        )

                val accountId =
                    principal.payload
                        .getClaim("userId")
                        .asInt()
                        ?.takeIf { id ->
                            id > 0
                        }
                        ?: return@post call.respond(
                            HttpStatusCode.Unauthorized,
                            ComplaintActionResponse(
                                success = false,
                                message = "A valid account ID is required."
                            )
                        )

                val role =
                    principal.payload
                        .getClaim("role")
                        .asString()
                        ?.trim()
                        ?.lowercase()
                        .orEmpty()

                if (role != "student") {
                    return@post call.respond(
                        HttpStatusCode.Forbidden,
                        ComplaintActionResponse(
                            success = false,
                            message = "Only parent portal accounts can submit complaints."
                        )
                    )
                }

                try {
                    val tenant =
                        call.currentTenant()

                    val request =
                        call.receive<
                                CreateParentComplaintRequest
                                >()

                    val complaint =
                        ComplaintRepository
                            .createParentComplaint(
                                tenantSchema =
                                    tenant.tenantSchema,

                                authenticatedAccountId =
                                    accountId,

                                request =
                                    request
                            )

                    call.respond(
                        HttpStatusCode.Created,
                        ComplaintActionResponse(
                            success = true,
                            message = "Complaint submitted successfully.",
                            complaint = complaint
                        )
                    )
                } catch (
                    exception: IllegalArgumentException
                ) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ComplaintActionResponse(
                            success = false,
                            message =
                                exception.message
                                    ?: "Unable to submit complaint."
                        )
                    )
                } catch (exception: Exception) {
                    println(
                        "[ComplaintRoutes] Complaint creation failed: " +
                                "accountId=$accountId, " +
                                "reason=${exception.message}"
                    )

                    exception.printStackTrace()

                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ComplaintActionResponse(
                            success = false,
                            message = "Unable to submit complaint."
                        )
                    )
                }
            }

            get {
                val principal =
                    call.principal<JWTPrincipal>()
                        ?: return@get call.respond(
                            HttpStatusCode.Unauthorized,
                            ComplaintActionResponse(
                                success = false,
                                message = "Authentication required."
                            )
                        )

                val accountId =
                    principal.payload
                        .getClaim("userId")
                        .asInt()
                        ?.takeIf { id ->
                            id > 0
                        }
                        ?: return@get call.respond(
                            HttpStatusCode.Unauthorized,
                            ComplaintActionResponse(
                                success = false,
                                message = "A valid account ID is required."
                            )
                        )

                try {
                    val tenant =
                        call.currentTenant()

                    val complaints =
                        ComplaintRepository
                            .findParentComplaints(
                                tenantSchema =
                                    tenant.tenantSchema,

                                authenticatedAccountId =
                                    accountId
                            )

                    call.respond(
                        HttpStatusCode.OK,
                        complaints
                    )
                } catch (exception: Exception) {
                    println(
                        "[ComplaintRoutes] Parent complaint retrieval failed: " +
                                "accountId=$accountId, " +
                                "reason=${exception.message}"
                    )

                    exception.printStackTrace()

                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ComplaintActionResponse(
                            success = false,
                            message = "Unable to retrieve complaints."
                        )
                    )
                }
            }

            get("/{complaintId}") {
                val principal =
                    call.principal<JWTPrincipal>()
                        ?: return@get call.respond(
                            HttpStatusCode.Unauthorized,
                            ComplaintActionResponse(
                                success = false,
                                message = "Authentication required."
                            )
                        )

                val accountId =
                    principal.payload
                        .getClaim("userId")
                        .asInt()
                        ?: return@get call.respond(
                            HttpStatusCode.Unauthorized,
                            ComplaintActionResponse(
                                success = false,
                                message = "A valid account ID is required."
                            )
                        )

                val complaintId =
                    call.parameters["complaintId"]
                        ?.toIntOrNull()
                        ?.takeIf { id ->
                            id > 0
                        }
                        ?: return@get call.respond(
                            HttpStatusCode.BadRequest,
                            ComplaintActionResponse(
                                success = false,
                                message = "A valid complaint ID is required."
                            )
                        )

                try {
                    val tenant =
                        call.currentTenant()

                    val complaint =
                        ComplaintRepository
                            .findParentComplaint(
                                tenantSchema =
                                    tenant.tenantSchema,

                                authenticatedAccountId =
                                    accountId,

                                complaintId =
                                    complaintId
                            )

                    call.respond(
                        HttpStatusCode.OK,
                        complaint
                    )
                } catch (
                    exception: IllegalArgumentException
                ) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        ComplaintActionResponse(
                            success = false,
                            message =
                                exception.message
                                    ?: "You cannot access this complaint."
                        )
                    )
                }
            }

            post("/{complaintId}/replies") {
                val principal =
                    call.principal<JWTPrincipal>()
                        ?: return@post call.respond(
                            HttpStatusCode.Unauthorized,
                            ComplaintActionResponse(
                                success = false,
                                message = "Authentication required."
                            )
                        )

                val accountId =
                    principal.payload
                        .getClaim("userId")
                        .asInt()
                        ?: return@post call.respond(
                            HttpStatusCode.Unauthorized,
                            ComplaintActionResponse(
                                success = false,
                                message = "A valid account ID is required."
                            )
                        )

                val complaintId =
                    call.parameters["complaintId"]
                        ?.toIntOrNull()
                        ?.takeIf { id ->
                            id > 0
                        }
                        ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            ComplaintActionResponse(
                                success = false,
                                message = "A valid complaint ID is required."
                            )
                        )

                try {
                    val tenant =
                        call.currentTenant()

                    val request =
                        call.receive<
                                CreateComplaintReplyRequest
                                >()

                    val reply =
                        ComplaintRepository
                            .createParentReply(
                                tenantSchema =
                                    tenant.tenantSchema,

                                authenticatedAccountId =
                                    accountId,

                                complaintId =
                                    complaintId,

                                request =
                                    request
                            )

                    call.respond(
                        HttpStatusCode.Created,
                        ComplaintReplyActionResponse(
                            success = true,
                            message = "Reply sent successfully.",
                            reply = reply
                        )
                    )
                } catch (
                    exception: IllegalArgumentException
                ) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ComplaintActionResponse(
                            success = false,
                            message =
                                exception.message
                                    ?: "Unable to send reply."
                        )
                    )
                }
            }
        }

        route("/admin/complaints") {
            get {
                val principal =
                    call.principal<JWTPrincipal>()
                        ?: return@get call.respond(
                            HttpStatusCode.Unauthorized,
                            ComplaintActionResponse(
                                success = false,
                                message = "Authentication required."
                            )
                        )

                val adminAccountId =
                    principal.payload
                        .getClaim("userId")
                        .asInt()
                        ?: return@get call.respond(
                            HttpStatusCode.Unauthorized,
                            ComplaintActionResponse(
                                success = false,
                                message = "A valid administrator account ID is required."
                            )
                        )

                val role =
                    principal.payload
                        .getClaim("role")
                        .asString()
                        ?.trim()
                        ?.lowercase()
                        .orEmpty()

                val allowedAdminRoles =
                    setOf(
                        "admin",
                        "super_admin",
                        "administrator",
                        "principal"
                    )

                if (role !in allowedAdminRoles) {
                    return@get call.respond(
                        HttpStatusCode.Forbidden,
                        ComplaintActionResponse(
                            success = false,
                            message = "You are not authorized to manage complaints."
                        )
                    )
                }

                val tenant =
                    call.currentTenant()

                val complaints =
                    ComplaintRepository
                        .findAdminComplaints(
                            tenantSchema =
                                tenant.tenantSchema,

                            authenticatedAdminAccountId =
                                adminAccountId,

                            statusFilter =
                                call.request
                                    .queryParameters["status"]
                        )

                call.respond(
                    HttpStatusCode.OK,
                    complaints
                )
            }

            get("/{complaintId}") {
                val context =
                    requireAdminContext()
                        ?: return@get

                val complaintId =
                    call.parameters["complaintId"]
                        ?.toIntOrNull()
                        ?.takeIf { id ->
                            id > 0
                        }
                        ?: return@get call.respond(
                            HttpStatusCode.BadRequest,
                            ComplaintActionResponse(
                                success = false,
                                message = "A valid complaint ID is required."
                            )
                        )

                val complaint =
                    ComplaintRepository
                        .findAdminComplaint(
                            tenantSchema =
                                context.tenantSchema,

                            authenticatedAdminAccountId =
                                context.accountId,

                            complaintId =
                                complaintId
                        )

                call.respond(
                    HttpStatusCode.OK,
                    complaint
                )
            }

            post("/{complaintId}/replies") {
                val context =
                    requireAdminContext()
                        ?: return@post

                val complaintId =
                    call.parameters["complaintId"]
                        ?.toIntOrNull()
                        ?.takeIf { id ->
                            id > 0
                        }
                        ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            ComplaintActionResponse(
                                success = false,
                                message = "A valid complaint ID is required."
                            )
                        )

                try {
                    val request =
                        call.receive<
                                CreateAdminComplaintReplyRequest
                                >()

                    val reply =
                        ComplaintRepository
                            .createAdminReply(
                                tenantSchema =
                                    context.tenantSchema,

                                authenticatedAdminAccountId =
                                    context.accountId,

                                complaintId =
                                    complaintId,

                                request =
                                    request
                            )

                    call.respond(
                        HttpStatusCode.Created,
                        ComplaintReplyActionResponse(
                            success = true,
                            message =
                                if (reply.isInternal) {
                                    "Internal note added successfully."
                                } else {
                                    "Reply sent successfully."
                                },

                            reply =
                                reply
                        )
                    )
                } catch (
                    exception: IllegalArgumentException
                ) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ComplaintActionResponse(
                            success = false,
                            message =
                                exception.message
                                    ?: "Unable to create reply."
                        )
                    )
                }
            }

            patch("/{complaintId}/status") {
                val context =
                    requireAdminContext()
                        ?: return@patch

                val complaintId =
                    call.parameters["complaintId"]
                        ?.toIntOrNull()
                        ?.takeIf { id ->
                            id > 0
                        }
                        ?: return@patch call.respond(
                            HttpStatusCode.BadRequest,
                            ComplaintActionResponse(
                                success = false,
                                message = "A valid complaint ID is required."
                            )
                        )

                try {
                    val request =
                        call.receive<
                                UpdateComplaintStatusRequest
                                >()

                    val complaint =
                        ComplaintRepository
                            .updateComplaintStatus(
                                tenantSchema =
                                    context.tenantSchema,

                                authenticatedAdminAccountId =
                                    context.accountId,

                                complaintId =
                                    complaintId,

                                status =
                                    request.status
                            )

                    call.respond(
                        HttpStatusCode.OK,
                        ComplaintActionResponse(
                            success = true,
                            message = "Complaint status updated.",
                            complaint = complaint
                        )
                    )
                } catch (
                    exception: IllegalArgumentException
                ) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ComplaintActionResponse(
                            success = false,
                            message =
                                exception.message
                                    ?: "Unable to update status."
                        )
                    )
                }
            }

            patch("/{complaintId}/assign") {
                val context =
                    requireAdminContext()
                        ?: return@patch

                val complaintId =
                    call.parameters["complaintId"]
                        ?.toIntOrNull()
                        ?.takeIf { id ->
                            id > 0
                        }
                        ?: return@patch call.respond(
                            HttpStatusCode.BadRequest,
                            ComplaintActionResponse(
                                success = false,
                                message = "A valid complaint ID is required."
                            )
                        )

                try {
                    val request =
                        call.receive<
                                AssignComplaintRequest
                                >()

                    val complaint =
                        ComplaintRepository
                            .assignComplaint(
                                tenantSchema =
                                    context.tenantSchema,

                                authenticatedAdminAccountId =
                                    context.accountId,

                                complaintId =
                                    complaintId,

                                request =
                                    request
                            )

                    call.respond(
                        HttpStatusCode.OK,
                        ComplaintActionResponse(
                            success = true,
                            message = "Complaint assignment updated.",
                            complaint = complaint
                        )
                    )
                } catch (
                    exception: IllegalArgumentException
                ) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ComplaintActionResponse(
                            success = false,
                            message =
                                exception.message
                                    ?: "Unable to assign complaint."
                        )
                    )
                }
            }
        }
    }
}

private data class AdminComplaintContext(
    val accountId: Int,
    val tenantSchema: String
)

private suspend fun io.ktor.server.routing.RoutingContext
        .requireAdminContext(): AdminComplaintContext? {
    val principal =
        call.principal<JWTPrincipal>()
            ?: run {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ComplaintActionResponse(
                        success = false,
                        message = "Authentication required."
                    )
                )

                return null
            }

    val accountId =
        principal.payload
            .getClaim("userId")
            .asInt()
            ?.takeIf { id ->
                id > 0
            }
            ?: run {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ComplaintActionResponse(
                        success = false,
                        message = "A valid administrator account ID is required."
                    )
                )

                return null
            }

    val role =
        principal.payload
            .getClaim("role")
            .asString()
            ?.trim()
            ?.lowercase()
            .orEmpty()

    val allowedRoles =
        setOf(
            "admin",
            "super_admin",
            "administrator",
            "principal"
        )

    if (role !in allowedRoles) {
        call.respond(
            HttpStatusCode.Forbidden,
            ComplaintActionResponse(
                success = false,
                message = "You are not authorized to manage complaints."
            )
        )

        return null
    }

    val tenant =
        call.currentTenant()

    return AdminComplaintContext(
        accountId =
            accountId,

        tenantSchema =
            tenant.tenantSchema
    )
}