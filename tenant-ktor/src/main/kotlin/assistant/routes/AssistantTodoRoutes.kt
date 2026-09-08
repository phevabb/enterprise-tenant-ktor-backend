package assistant.routes

import assistant.repos.AssistantTodoRepository
import assistant.requests.AssistantTodoRequest
import com.example.tenant.tenantTransaction
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import tenant.getTenantSchema

fun Route.assistantTodoRoutes() {


    put("/{id}") {

        val tenantSchema =
            call.getTenantSchema()

        val id =
            call.parameters["id"]!!.toInt()

        val request =
            call.receive<AssistantTodoRequest>()

        val updated =
            tenantTransaction(
                tenantSchema
            ) {

                AssistantTodoRepository.update(
                    id,
                    request
                )
            }

        if (!updated) {

            call.respond(
                HttpStatusCode.NotFound,
                "Todo not found"
            )

            return@put
        }

        call.respond(
            HttpStatusCode.OK,
            "Todo updated successfully"
        )
    }




    get {

        val tenantSchema =
            call.getTenantSchema()

        val todos =
            tenantTransaction(
                tenantSchema
            ) {

                AssistantTodoRepository.getAll()
            }

        call.respond(
            todos
        )
    }

    get("/{id}") {

        val tenantSchema =
            call.getTenantSchema()

        val id =
            call.parameters["id"]!!.toInt()

        val todo =
            tenantTransaction(
                tenantSchema
            ) {

                AssistantTodoRepository.getById(
                    id
                )
            }

        if (todo == null) {

            call.respond(
                HttpStatusCode.NotFound,
                "Todo not found"
            )

            return@get
        }

        call.respond(
            todo
        )
    }

    post {

        val tenantSchema =
            call.getTenantSchema()

        val request =
            call.receive<AssistantTodoRequest>()

        val id =
            tenantTransaction(
                tenantSchema
            ) {

                AssistantTodoRepository.create(
                    request
                )
            }

        call.respond(
            HttpStatusCode.Created,
            id
        )
    }

    patch("/{id}/toggle") {

        val tenantSchema =
            call.getTenantSchema()

        val id =
            call.parameters["id"]!!.toInt()

        val updated =
            tenantTransaction(
                tenantSchema
            ) {

                AssistantTodoRepository.toggleCompleted(
                    id
                )
            }

        if (!updated) {

            call.respond(
                HttpStatusCode.NotFound,
                "Todo not found"
            )

            return@patch
        }

        call.respond(
            HttpStatusCode.OK,
            "Todo updated successfully"
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

                AssistantTodoRepository.delete(
                    id
                )
            }

        if (!deleted) {

            call.respond(
                HttpStatusCode.NotFound,
                "Todo not found"
            )

            return@delete
        }

        call.respond(
            HttpStatusCode.OK,
            "Todo deleted successfully"
        )
    }
}