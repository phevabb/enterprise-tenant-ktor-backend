package com.example.student.routes


import com.example.student.dtos.requests.CreateClassPromotionRequest
import com.example.student.dtos.requests.PatchClassPromotionRequest
import com.example.student.repos.ClassPromotionRepository
import com.example.tenant.currentTenant
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.response.*
import io.ktor.server.routing.delete
import io.ktor.server.routing.patch
import io.ktor.server.routing.post


fun Route.classPromotionRoutes(){
    val repo = ClassPromotionRepository()


    authenticate("auth-jwt") {

        get {
            val tenant = call.currentTenant()

            val proms = repo.findAll(
                tenantSchema = tenant.tenantSchema
            )

            call.respond(HttpStatusCode.OK, proms)
        }

        get("{id}") {
            val id = call.parameters["id"]!!.toInt()

            val tenant = call.currentTenant()

            val item = repo.findById(
                tenantSchema = tenant.tenantSchema,
                id = id
            ) ?: return@get call.respond(HttpStatusCode.NotFound)

            call.respond(HttpStatusCode.OK, item)
        }

        post {
            val req = call.receive<CreateClassPromotionRequest>()

            val tenant = call.currentTenant()

            val created = repo.create(
                tenantSchema = tenant.tenantSchema,
                req = req
            )

            call.respond(HttpStatusCode.Created, created)
        }

        patch("{id}") {
            val id = call.parameters["id"]!!.toInt()

            val req = call.receive<PatchClassPromotionRequest>()

            val tenant = call.currentTenant()

            val updated = repo.patch(
                tenantSchema = tenant.tenantSchema,
                id = id,
                req = req
            ) ?: return@patch call.respond(HttpStatusCode.NotFound)

            call.respond(HttpStatusCode.OK, updated)
        }

        delete("{id}") {
            val id = call.parameters["id"]!!.toInt()

            val tenant = call.currentTenant()

            val ok = repo.delete(
                tenantSchema = tenant.tenantSchema,
                id = id
            )

            if (!ok) {
                return@delete call.respond(HttpStatusCode.NotFound)
            }

            call.respond(HttpStatusCode.NoContent)
        }

    }

}