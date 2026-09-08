package birthday



import birthday.routes.birthdayRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.birthdayModule() {

    routing {

        route("/api") {

            route("/birthday") {

                birthdayRoutes()
            }
        }
    }
}