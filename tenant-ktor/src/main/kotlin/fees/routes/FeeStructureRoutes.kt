package com.example.fees.routes

import com.example.fees.dtos.requests.CreateFeeStructureRequest
import com.example.fees.dtos.requests.PatchFeeStructureRequest
import com.example.fees.repos.FeeStructureRepository
import com.example.student.dtos.PaginatedResponse
import com.example.student.dtos.PaginationMeta
import com.example.tenant.currentTenant
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.patch
import io.ktor.server.routing.post



fun Route.feeStructureRoutes(){



    authenticate("auth-jwt") {



        get {
            val tenant = call.currentTenant()
            val tenantSchema = tenant.tenantSchema

            val feeStructures = FeeStructureRepository.findAll(tenantSchema)
            call.respond(HttpStatusCode.OK, feeStructures)
        }

        get("/paginated") {
            val tenant = call.currentTenant()
            val tenantSchema = tenant.tenantSchema
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val limit = 10
            val search = call.request.queryParameters["search"]

            val (feeStructures, total) =
                FeeStructureRepository.findAllPaginated(
                    tenantSchema = tenantSchema,
                    page = page,
                    limit = limit,
                    search = search)

            val response = PaginatedResponse(
                data = feeStructures,
                meta = PaginationMeta(
                    page = page,
                    limit = limit,
                    total = total,
                    totalPages = ((total + limit - 1) / limit).toInt()
                )
            )

            call.respond(HttpStatusCode.OK, response)
        }



        post {

            try {

                val tenant =
                    call.currentTenant()

                val tenantSchema =
                    tenant.tenantSchema

                val req =
                    call.receive<CreateFeeStructureRequest>()

                println(
                    "new request is $req"
                )

                val c =
                    FeeStructureRepository.create(
                        tenantSchema = tenantSchema,
                        academicYearId = req.academic_year_id,
                        gradeClassId = req.grade_class_id,
                        termId = req.term_id,
                        amount = req.amount,
                        isDiscounted = req.is_discounted
                    )

                /*
                 * Important:
                 * Discounted fee structures are special.
                 * Do not auto-generate fee records for all students.
                 */
                if (!req.is_discounted) {

                    val generated =
                        FeeStructureRepository.generateStudentFeeRecordsForFeeStructure(
                            tenantSchema = tenantSchema,
                            feeStructureId = c.id
                        )

                    println(
                        "Student fee records generated: $generated"
                    )

                } else {

                    println(
                        "Skipped auto-generating student fee records because fee structure is discounted."
                    )
                }

                call.respond(
                    HttpStatusCode.Created,
                    c
                )

            } catch (e: Exception) {

                e.printStackTrace()

                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "message" to (
                                e.message
                                    ?: "Unable to create fee structure."
                                )
                    )
                )
            }
        }



        delete("{id}") {
            val tenant = call.currentTenant()
            val tenantSchema = tenant.tenantSchema

            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid id")
                return@delete
            }
            val ok = FeeStructureRepository.delete(tenantSchema,id )
            if (!ok) {
                call.respond(HttpStatusCode.NotFound, "Fee structure Not found")
                return@delete
            } else {
                call.respond(HttpStatusCode.NoContent)
            }
        }

        patch("{id}") {
            val tenant = call.currentTenant()
            val tenantSchema = tenant.tenantSchema

            val id = call.parameters["id"]?.toIntOrNull()

            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid id")
                return@patch
            }

            val req = call.receive<PatchFeeStructureRequest>()

            val updated = FeeStructureRepository.patch(
                tenantSchema = tenantSchema,
                id = id,
                amount = req.amount,
                isDiscounted = req.is_discounted
            )

            if (updated == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid id")
            } else {
                call.respond(HttpStatusCode.OK, updated)
            }
        }

    }



}