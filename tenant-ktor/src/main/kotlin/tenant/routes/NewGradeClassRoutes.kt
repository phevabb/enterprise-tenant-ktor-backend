package tenant.routes

import com.example.student.repos.NewGradeClassRepository
import com.example.tenant.currentTenant
import tenant.dto.response.GradeClassWithStudentCountResponse
import tenant.repository.GradeClassStudentCountRepository

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.newGradeClassRoutes() {

    route("/new") {

        /*
         * Existing endpoint.
         * Do not change this because other APIs depend on its response.
         */
        get {

            val tenant =
                call.currentTenant()

            val gradeClasses =
                NewGradeClassRepository.findAll(
                    tenantSchema = tenant.tenantSchema
                )

            call.respond(
                HttpStatusCode.OK,
                gradeClasses
            )
        }

        /*
         * New endpoint with student counts.
         */
        get("/with-student-count") {

            try {

                val tenant =
                    call.currentTenant()

                val tenantSchema =
                    tenant.tenantSchema

                val gradeClasses =
                    NewGradeClassRepository.findAll(
                        tenantSchema = tenantSchema
                    )

                val studentCounts =
                    GradeClassStudentCountRepository
                        .countStudentsByClass(
                            tenantSchema = tenantSchema
                        )

                val response =
                    gradeClasses.map { gradeClass ->

                        GradeClassWithStudentCountResponse(
                            id = gradeClass.id,
                            name = gradeClass.name,
                            isActive = gradeClass.is_active,
                            categoryId = gradeClass.categoryId,
                            categoryName = gradeClass.categoryName,
                            studentCount = studentCounts[
                                gradeClass.id
                            ] ?: 0
                        )
                    }

                call.respond(
                    HttpStatusCode.OK,
                    response
                )

            } catch (e: Exception) {

                println(
                    "[classes-with-student-count] Failed: ${e.message}"
                )

                e.printStackTrace()

                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "message" to (
                                e.message
                                    ?: "Unable to retrieve classes and student counts."
                                )
                    )
                )
            }
        }
    }
}