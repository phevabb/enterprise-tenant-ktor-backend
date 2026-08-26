package chat.module



import chat.routes.chatRoutes
import chat.routes.chatSocketRoutes
import chat.routes.staffAssignedClassRoutes
import chat.routes.studentClassTeacherRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

fun Application.configureRouting() {

    routing {
        chatRoutes()
        chatSocketRoutes()
        studentClassTeacherRoutes()
        staffAssignedClassRoutes()
    }
}