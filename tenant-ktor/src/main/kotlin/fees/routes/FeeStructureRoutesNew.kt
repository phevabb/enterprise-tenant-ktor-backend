package fees.routes


import fees.dtos.requests.CreateFeeStructureRequest
import fees.dtos.responses.FeeStructureCreateResponse
import fees.repos.FeeStructureRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

fun Route.feeStructureRoutes() {

    post("/fee-structures") {

        try {

            val tenantSchema =
                call.request.headers["X-Tenant-Schema"]
                    ?: throw IllegalArgumentException(
                        "Tenant schema is required."
                    )

            val request =
                call.receive<CreateFeeStructureRequest>()

            val response =
                FeeStructureRepository.createFeeStructureAndGenerateStudentRecords(
                    tenantSchema = tenantSchema,
                    request = request
                )

            call.respond(
                HttpStatusCode.Created,
                response
            )

        } catch (e: IllegalArgumentException) {

            call.respond(
                HttpStatusCode.BadRequest,
                FeeStructureCreateResponse(
                    success = false,
                    message = e.message ?: "Invalid fee structure request.",
                    feeStructureId = 0,
                    studentsFound = 0,
                    recordsCreated = 0,
                    recordsSkipped = 0
                )
            )

        } catch (e: Exception) {

            e.printStackTrace()

            call.respond(
                HttpStatusCode.InternalServerError,
                FeeStructureCreateResponse(
                    success = false,
                    message = e.message ?: "Unable to create fee structure.",
                    feeStructureId = 0,
                    studentsFound = 0,
                    recordsCreated = 0,
                    recordsSkipped = 0
                )
            )
        }
    }
}