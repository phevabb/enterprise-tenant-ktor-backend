package chat.routes

import chat.repositories.StudentClassTeacherRepository
import com.example.tenant.currentTenant
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable

@Serializable
data class StudentTeacherRouteError(
    val success: Boolean = false,
    val message: String
)

fun Route.studentClassTeacherRoutes() {

    authenticate("auth-jwt") {

        route("/chat/students") {

            get("/{studentUserId}/class-teachers") {
                try {
                    val tenant =
                        call.currentTenant()

                    val studentUserId =
                        call.parameters[
                            "studentUserId"
                        ]
                            ?.trim()
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: return@get call.respond(
                                status =
                                    HttpStatusCode.BadRequest,

                                message =
                                    StudentTeacherRouteError(
                                        message =
                                            "Student user ID is required."
                                    )
                            )

                    val response =
                        StudentClassTeacherRepository
                            .findTeachersByStudentUserId(
                                tenantSchema =
                                    tenant.tenantSchema,

                                studentUserId =
                                    studentUserId
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
                            StudentTeacherRouteError(
                                message =
                                    exception.message
                                        ?: "Unable to find the student's class teacher."
                            )
                    )
                } catch (exception: Exception) {
                    println(
                        "[StudentClassTeacherRoutes] " +
                                "Failed to find teacher: " +
                                exception.message
                    )

                    exception.printStackTrace()

                    call.respond(
                        status =
                            HttpStatusCode.InternalServerError,

                        message =
                            StudentTeacherRouteError(
                                message =
                                    "Unable to retrieve the student's class teacher."
                            )
                    )
                }
            }
        }
    }
}