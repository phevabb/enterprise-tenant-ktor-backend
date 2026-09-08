package assistant


import assistant.routes.assistantNoteRoutes
import assistant.routes.assistantTodoRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.assistantModule() {

    routing {

        route("/api/assistant") {

            route("/notes") {
                assistantNoteRoutes()
            }

            route("/todos") {
                assistantTodoRoutes()
            }
        }
    }
}