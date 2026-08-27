package complaints

import complaints.routes.complaintRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

fun Application.complaintModule() {
    routing {
        complaintRoutes()
    }
}