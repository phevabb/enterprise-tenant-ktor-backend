package assistant.routes


import assistant.repos.AssistantNoteRepository
import assistant.requests.AssistantNoteRequest
import com.example.tenant.tenantTransaction
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import tenant.getTenantSchema

fun Route.assistantNoteRoutes() {

    get {

        val tenantSchema =
            call.getTenantSchema()

        val notes =
            tenantTransaction(
                tenantSchema
            ) {

                AssistantNoteRepository.getAll()
            }

        call.respond(
            notes
        )
    }

    get("/{id}") {

        val tenantSchema =
            call.getTenantSchema()

        val id =
            call.parameters["id"]!!.toInt()

        val note =
            tenantTransaction(
                tenantSchema
            ) {

                AssistantNoteRepository
                    .getById(id)
            }

        call.respond(
            note ?: HttpStatusCode.NotFound
        )
    }

    post {

        val tenantSchema =
            call.getTenantSchema()

        val request =
            call.receive<AssistantNoteRequest>()

        val id =
            tenantTransaction(
                tenantSchema
            ) {

                AssistantNoteRepository.create(
                    request
                )
            }

        call.respond(
            HttpStatusCode.Created,
            id
        )
    }

    put("/{id}") {

        val tenantSchema =
            call.getTenantSchema()

        val id =
            call.parameters["id"]!!.toInt()

        val request =
            call.receive<AssistantNoteRequest>()

        val updated =
            tenantTransaction(
                tenantSchema
            ) {

                AssistantNoteRepository.update(
                    id,
                    request
                )
            }

        if (!updated) {

            call.respond(
                HttpStatusCode.NotFound,
                "Note not found"
            )

            return@put
        }

        call.respond(
            HttpStatusCode.OK,
            "Note updated successfully"
        )
    }


    delete("/{id}") {

        val tenantSchema =
            call.getTenantSchema()

        val id =
            call.parameters["id"]!!.toInt()

        val deleted =
            tenantTransaction(
                tenantSchema
            ) {

                AssistantNoteRepository.delete(
                    id
                )
            }

        if (!deleted) {

            call.respond(
                HttpStatusCode.NotFound,
                "Note not found"
            )

            return@delete
        }

        call.respond(
            HttpStatusCode.OK,
            "Note deleted successfully"
        )
    }


}


