package attendance.route

import attendance.dto.AttendanceErrorResponse
import attendance.dto.DeleteAttendanceResponse
import attendance.dto.SaveAttendanceRequest
import attendance.repo.AttendanceRepository
import attendance.service.AttendanceService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import attendance.dto.UpdateAttendanceRequest
import com.example.config.DatabaseFactory.dbQuery
import com.example.tenant.tables.TenantsTable
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import org.apache.http.client.methods.RequestBuilder.post
import org.jetbrains.exposed.sql.selectAll
import tenant.TenantContextKey
import java.time.LocalDate

fun Application.configureAttendanceRoutes() {
    val attendanceRepository = AttendanceRepository()

    val attendanceService = AttendanceService(
        attendanceRepository = attendanceRepository,
    )

    routing {
        route("/api/attendance") {

            patch("/{attendanceId}") {
                try {
                    val tenantCode =
                        call.request.headers["X-Tenant-Code"]
                            ?.trim()
                            ?.takeIf { it.isNotEmpty() }
                            ?: throw IllegalArgumentException(
                                "X-Tenant-Code header is required.",
                            )

                    require(
                        tenantCode.matches(
                            Regex("^[a-zA-Z0-9_]+$"),
                        ),
                    ) {
                        "Invalid tenant code."
                    }

                    val tenantSchema =
                        "tenant_$tenantCode"

                    val attendanceId =
                        call.parameters["attendanceId"]
                            ?.toLongOrNull()
                            ?: throw IllegalArgumentException(
                                "A valid attendance ID is required.",
                            )

                    val request =
                        call.receive<UpdateAttendanceRequest>()

                    val response =
                        attendanceService.updateAttendanceRecord(
                            attendanceId = attendanceId,
                            request = request,
                            tenantSchema = tenantSchema,
                        )

                    call.respond(
                        status =
                            if (response.success) {
                                HttpStatusCode.OK
                            } else {
                                HttpStatusCode.NotFound
                            },
                        message = response,
                    )
                } catch (error: IllegalArgumentException) {
                    println(
                        "Attendance update validation error: ${error.message}",
                    )

                    call.respond(
                        status = HttpStatusCode.BadRequest,
                        message = AttendanceErrorResponse(
                            message =
                                error.message
                                    ?: "Invalid attendance update request.",
                        ),
                    )
                } catch (error: Exception) {
                    println(
                        "Attendance update error: ${error.message}",
                    )

                    error.printStackTrace()

                    call.respond(
                        status = HttpStatusCode.InternalServerError,
                        message = AttendanceErrorResponse(
                            message =
                                "Unable to update attendance.",
                        ),
                    )
                }
            }





            post("/teacher/submit") {
                try {
                    val tenantCode =
                        call.request.headers["X-Tenant-Code"]
                            ?.trim()
                            ?.takeIf { it.isNotEmpty() }
                            ?: throw IllegalArgumentException(
                                "X-Tenant-Code header is required.",
                            )

                    require(
                        tenantCode.matches(
                            Regex("^[a-zA-Z0-9_]+$"),
                        ),
                    ) {
                        "Invalid tenant code."
                    }

                    val tenantSchema =
                        "tenant_$tenantCode"

                    val tenant =
                        dbQuery(
                            tenantSchema = tenantSchema,
                        ) {
                            TenantsTable
                                .selectAll()
                                .where {
                                    TenantsTable.tenantCode eq
                                            tenantCode
                                }
                                .limit(1)
                                .singleOrNull()
                        }
                            ?: throw IllegalArgumentException(
                                "No school was found for tenant code $tenantCode.",
                            )

                    val schoolId =
                        tenant[TenantsTable.id]

                    val schoolName =
                        tenant[TenantsTable.schoolName]

                    require(schoolId > 0) {
                        "A valid school could not be resolved from the tenant code."
                    }

                    val request =
                        call.receive<SaveAttendanceRequest>()

                    println(
                        "[ATTENDANCE] tenantCode=$tenantCode",
                    )

                    println(
                        "[ATTENDANCE] tenantSchema=$tenantSchema",
                    )

                    println(
                        "[ATTENDANCE] schoolId=$schoolId",
                    )

                    println(
                        "[ATTENDANCE] schoolName=$schoolName",
                    )

                    println(
                        "[ATTENDANCE] teacherId=${request.teacherId}",
                    )

                    println(
                        "[ATTENDANCE] classId=${request.classId}",
                    )

                    val response =
                        attendanceService.saveAttendance(
                            request = request,
                            schoolId = schoolId,
                            tenantSchema = tenantSchema,
                            markedBy = request.teacherId,
                        )

                    call.respond(
                        status = HttpStatusCode.OK,
                        message = response,
                    )
                } catch (error: IllegalArgumentException) {
                    println(
                        "Attendance validation error: ${error.message}",
                    )

                    call.respond(
                        status = HttpStatusCode.BadRequest,
                        message = AttendanceErrorResponse(
                            message =
                                error.message
                                    ?: "Invalid attendance request.",
                        ),
                    )
                } catch (error: Exception) {
                    println(
                        "Attendance submission error: ${error.message}",
                    )

                    error.printStackTrace()

                    call.respond(
                        status = HttpStatusCode.InternalServerError,
                        message = AttendanceErrorResponse(
                            message =
                                "Unable to save attendance.",
                        ),
                    )
                }
            }


            get("/class/{classId}") {
                try {
                    val schoolId =
                        call.request.queryParameters["schoolId"]
                            ?.toLongOrNull()
                            ?: throw IllegalArgumentException(
                                "A valid schoolId query parameter is required",
                            )

                    val classId =
                        call.parameters["classId"]
                            ?.toLongOrNull()
                            ?: throw IllegalArgumentException(
                                "A valid class ID is required",
                            )

                    val attendanceDate =
                        call.request.queryParameters["date"]
                            ?: throw IllegalArgumentException(
                                "The attendance date is required",
                            )

                    val session =
                        call.request.queryParameters["session"]
                            ?: "MORNING"

                    val response =
                        attendanceService.getClassAttendance(
                            schoolId = schoolId,
                            classId = classId,
                            attendanceDateValue = attendanceDate,
                            sessionValue = session,
                        )

                    call.respond(
                        status = HttpStatusCode.OK,
                        message = response,
                    )
                } catch (error: IllegalArgumentException) {
                    println(
                        "Class attendance validation error: ${error.message}",
                    )

                    call.respond(
                        status = HttpStatusCode.BadRequest,
                        message = AttendanceErrorResponse(
                            message = error.message
                                ?: "Invalid attendance request",
                        ),
                    )
                } catch (error: Exception) {
                    println(
                        "Class attendance retrieval error: ${error.message}",
                    )

                    call.respond(
                        status = HttpStatusCode.InternalServerError,
                        message = AttendanceErrorResponse(
                            message = "Unable to load class attendance",
                        ),
                    )
                }
            }

            get("/class/{classId}/summary") {
                try {
                    val schoolId =
                        call.request.queryParameters["schoolId"]
                            ?.toLongOrNull()
                            ?: throw IllegalArgumentException(
                                "A valid schoolId query parameter is required",
                            )

                    val classId =
                        call.parameters["classId"]
                            ?.toLongOrNull()
                            ?: throw IllegalArgumentException(
                                "A valid class ID is required",
                            )

                    val attendanceDate =
                        call.request.queryParameters["date"]
                            ?: throw IllegalArgumentException(
                                "The attendance date is required",
                            )

                    val session =
                        call.request.queryParameters["session"]
                            ?: "MORNING"

                    val response =
                        attendanceService
                            .getClassAttendanceSummary(
                                schoolId = schoolId,
                                classId = classId,
                                attendanceDateValue =
                                    attendanceDate,
                                sessionValue = session,
                            )

                    call.respond(
                        status = HttpStatusCode.OK,
                        message = response,
                    )
                } catch (error: IllegalArgumentException) {
                    println(
                        "Attendance summary validation error: ${error.message}",
                    )

                    call.respond(
                        status = HttpStatusCode.BadRequest,
                        message = AttendanceErrorResponse(
                            message = error.message
                                ?: "Invalid summary request",
                        ),
                    )
                } catch (error: Exception) {
                    println(
                        "Attendance summary error: ${error.message}",
                    )

                    call.respond(
                        status = HttpStatusCode.InternalServerError,
                        message = AttendanceErrorResponse(
                            message = "Unable to load attendance summary",
                        ),
                    )
                }
            }

            get("/student/{studentId}") {
                try {
                    val schoolId =
                        call.request.queryParameters["schoolId"]
                            ?.toLongOrNull()
                            ?: throw IllegalArgumentException(
                                "A valid schoolId query parameter is required",
                            )

                    val studentId =
                        call.parameters["studentId"]
                            ?.toLongOrNull()
                            ?: throw IllegalArgumentException(
                                "A valid student ID is required",
                            )

                    val startDate =
                        call.request.queryParameters["from"]
                            ?: throw IllegalArgumentException(
                                "The start date is required",
                            )

                    val endDate =
                        call.request.queryParameters["to"]
                            ?: throw IllegalArgumentException(
                                "The end date is required",
                            )

                    val response =
                        attendanceService.getStudentAttendance(
                            schoolId = schoolId,
                            studentId = studentId,
                            startDateValue = startDate,
                            endDateValue = endDate,
                        )

                    call.respond(
                        status = HttpStatusCode.OK,
                        message = response,
                    )
                } catch (error: IllegalArgumentException) {
                    println(
                        "Student attendance validation error: ${error.message}",
                    )

                    call.respond(
                        status = HttpStatusCode.BadRequest,
                        message = AttendanceErrorResponse(
                            message = error.message
                                ?: "Invalid attendance request",
                        ),
                    )
                } catch (error: Exception) {
                    println(
                        "Student attendance retrieval error: ${error.message}",
                    )

                    call.respond(
                        status = HttpStatusCode.InternalServerError,
                        message = AttendanceErrorResponse(
                            message = "Unable to load student attendance",
                        ),
                    )
                }
            }





            delete("/class/{classId}") {
                try {
                    val tenantCode =
                        call.request.headers["X-Tenant-Code"]
                            ?.trim()
                            ?.takeIf { it.isNotEmpty() }
                            ?: throw IllegalArgumentException(
                                "X-Tenant-Code header is required.",
                            )

                    require(
                        tenantCode.matches(
                            Regex("^[a-zA-Z0-9_]+$"),
                        ),
                    ) {
                        "Invalid tenant code."
                    }

                    val tenantSchema =
                        "tenant_$tenantCode"

                    val tenant =
                        dbQuery(
                            tenantSchema = tenantSchema,
                        ) {
                            TenantsTable
                                .selectAll()
                                .where {
                                    TenantsTable.tenantCode eq
                                            tenantCode
                                }
                                .limit(1)
                                .singleOrNull()
                        }
                            ?: throw IllegalArgumentException(
                                "No school was found for tenant code $tenantCode.",
                            )

                    val schoolId =
                        tenant[TenantsTable.id]
                            .toLong()

                    require(schoolId > 0) {
                        "A valid school could not be resolved from the tenant code."
                    }

                    val classId =
                        call.parameters["classId"]
                            ?.toLongOrNull()
                            ?: throw IllegalArgumentException(
                                "A valid class ID is required.",
                            )

                    require(classId > 0) {
                        "A valid class ID is required."
                    }

                    val attendanceDate =
                        call.request.queryParameters["date"]
                            ?.trim()
                            ?.takeIf { it.isNotEmpty() }
                            ?: throw IllegalArgumentException(
                                "The attendance date is required.",
                            )

                    val session =
                        call.request.queryParameters["session"]
                            ?.trim()
                            ?.uppercase()
                            ?.takeIf { it.isNotEmpty() }
                            ?: "MORNING"

                    println(
                        "[ATTENDANCE DELETE] tenantCode=$tenantCode",
                    )

                    println(
                        "[ATTENDANCE DELETE] tenantSchema=$tenantSchema",
                    )

                    println(
                        "[ATTENDANCE DELETE] schoolId=$schoolId",
                    )

                    println(
                        "[ATTENDANCE DELETE] classId=$classId",
                    )

                    println(
                        "[ATTENDANCE DELETE] date=$attendanceDate",
                    )

                    println(
                        "[ATTENDANCE DELETE] session=$session",
                    )

                    val deletedRecords =
                        attendanceService.deleteClassAttendance(
                            schoolId = schoolId,
                            classId = classId,
                            attendanceDateValue = attendanceDate,
                            sessionValue = session,
                            tenantSchema = tenantSchema,
                        )

                    call.respond(
                        status = HttpStatusCode.OK,
                        message = DeleteAttendanceResponse(
                            success = true,
                            message =
                                if (deletedRecords > 0) {
                                    "Attendance deleted successfully."
                                } else {
                                    "No matching attendance records were found."
                                },
                            deletedRecords = deletedRecords,
                        ),
                    )

                } catch (error: IllegalArgumentException) {
                    println(
                        "Attendance deletion validation error: ${error.message}",
                    )

                    call.respond(
                        status = HttpStatusCode.BadRequest,
                        message = AttendanceErrorResponse(
                            message =
                                error.message
                                    ?: "Invalid attendance deletion request.",
                        ),
                    )
                } catch (error: Exception) {
                    println(
                        "Attendance deletion error: ${error.message}",
                    )

                    error.printStackTrace()

                    call.respond(
                        status = HttpStatusCode.InternalServerError,
                        message = AttendanceErrorResponse(
                            message =
                                "Unable to delete attendance.",
                        ),
                    )
                }
            }






            get("/teacher/history") {
                try {
                    val tenantCode =
                        call.request.headers["X-Tenant-Code"]
                            ?.trim()
                            ?.takeIf { it.isNotEmpty() }
                            ?: throw IllegalArgumentException(
                                "X-Tenant-Code header is required.",
                            )

                    require(
                        tenantCode.matches(
                            Regex("^[a-zA-Z0-9_]+$"),
                        ),
                    ) {
                        "Invalid tenant code."
                    }

                    val tenantSchema =
                        "tenant_$tenantCode"

                    val tenant =
                        dbQuery(
                            tenantSchema = tenantSchema,
                        ) {
                            TenantsTable
                                .selectAll()
                                .where {
                                    TenantsTable.tenantCode eq
                                            tenantCode
                                }
                                .limit(1)
                                .singleOrNull()
                        }
                            ?: throw IllegalArgumentException(
                                "No school was found for tenant code $tenantCode.",
                            )

                    val schoolId =
                        tenant[TenantsTable.id]



                    val teacherId =
                        call.request.queryParameters["teacherId"]
                            ?.toLongOrNull()
                            ?: throw IllegalArgumentException(
                                "A valid teacherId query parameter is required.",
                            )

                    val fromValue =
                        call.request.queryParameters["from"]
                            ?: throw IllegalArgumentException(
                                "The start date is required.",
                            )

                    val toValue =
                        call.request.queryParameters["to"]
                            ?: throw IllegalArgumentException(
                                "The end date is required.",
                            )

                    val fromDate =
                        try {
                            LocalDate.parse(fromValue)
                        } catch (error: Exception) {
                            throw IllegalArgumentException(
                                "The start date must use YYYY-MM-DD format.",
                            )
                        }

                    val toDate =
                        try {
                            LocalDate.parse(toValue)
                        } catch (error: Exception) {
                            throw IllegalArgumentException(
                                "The end date must use YYYY-MM-DD format.",
                            )
                        }

                    require(!toDate.isBefore(fromDate)) {
                        "The end date cannot be before the start date."
                    }

                    val response =
                        attendanceService.getTeacherAttendanceHistory(
                            schoolId = schoolId.toLong(),
                            teacherId = teacherId,
                            fromDate = fromDate,
                            toDate = toDate,
                            tenantSchema = tenantSchema,
                        )

                    call.respond(
                        status = HttpStatusCode.OK,
                        message = response,
                    )
                } catch (error: IllegalArgumentException) {
                    println(
                        "Teacher attendance history validation error: ${error.message}",
                    )

                    call.respond(
                        status = HttpStatusCode.BadRequest,
                        message = AttendanceErrorResponse(
                            message =
                                error.message
                                    ?: "Invalid attendance history request.",
                        ),
                    )
                } catch (error: Exception) {
                    println(
                        "Teacher attendance history error: ${error.message}",
                    )

                    error.printStackTrace()

                    call.respond(
                        status = HttpStatusCode.InternalServerError,
                        message = AttendanceErrorResponse(
                            message =
                                "Unable to load attendance history.",
                        ),
                    )
                }
            }

        }
    }
}














