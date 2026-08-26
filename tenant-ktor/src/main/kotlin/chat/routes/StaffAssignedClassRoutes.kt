package chat.routes


import chat.repositories.StaffAssignedClassRepository
import com.example.tenant.currentTenant
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable

@Serializable
data class StaffAssignedClassErrorResponse(
    val success: Boolean = false,
    val message: String
)

fun Route.staffAssignedClassRoutes() {

    authenticate("auth-jwt") {

        route("/staff/chat") {

            get(
                "/assigned-class/{staffUserId}"
            ) {
                try {
                    val tenant =
                        call.currentTenant()

                    val principal =
                        call.principal<JWTPrincipal>()
                            ?: return@get call.respond(
                                status =
                                    HttpStatusCode.Unauthorized,

                                message =
                                    StaffAssignedClassErrorResponse(
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
                                    StaffAssignedClassErrorResponse(
                                        message =
                                            "A valid authenticated account ID is required."
                                    )
                            )

                    val staffUserId =
                        call.parameters[
                            "staffUserId"
                        ]
                            ?.trim()
                            ?.takeIf { userId ->
                                userId.isNotBlank()
                            }
                            ?: return@get call.respond(
                                status =
                                    HttpStatusCode.BadRequest,

                                message =
                                    StaffAssignedClassErrorResponse(
                                        message =
                                            "Staff user ID is required."
                                    )
                            )

                    val response =
                        StaffAssignedClassRepository
                            .findAssignedClassAndStudents(
                                tenantSchema =
                                    tenant.tenantSchema,

                                authenticatedAccountId =
                                    authenticatedAccountId,

                                staffUserId =
                                    staffUserId
                            )

                    call.respond(
                        status =
                            HttpStatusCode.OK,

                        message =
                            response
                    )
                } catch (
                    exception: IllegalArgumentException
                ) {
                    call.respond(
                        status =
                            HttpStatusCode.BadRequest,

                        message =
                            StaffAssignedClassErrorResponse(
                                message =
                                    exception.message
                                        ?: "Unable to retrieve the assigned class."
                            )
                    )
                } catch (exception: Exception) {
                    println(
                        "[StaffAssignedClassRoutes] " +
                                "Failed to retrieve assigned class: " +
                                exception.message
                    )

                    exception.printStackTrace()

                    call.respond(
                        status =
                            HttpStatusCode.InternalServerError,

                        message =
                            StaffAssignedClassErrorResponse(
                                message =
                                    "Unable to retrieve the assigned class and students."
                            )
                    )
                }
            }
        }
    }
}