package com.example.student.routes


import com.example.student.dtos.requests.CreateClassPromotionRequest
import com.example.student.dtos.requests.PatchClassPromotionRequest
import com.example.student.repos.ClassPromotionRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.response.*
import io.ktor.server.routing.delete
import io.ktor.server.routing.patch
import io.ktor.server.routing.post


fun Route.classPromotionRoutes(){
    val repo = ClassPromotionRepository()

    get{
        val proms = repo.findAll()
        call.respond(HttpStatusCode.OK, proms)
    }

    get("/class-promotions/{id}") {
        val id = call.parameters["id"]!!.toInt()

        val item = repo.findById(id) ?: return@get call.respond(HttpStatusCode.NotFound)
        call.respond(HttpStatusCode.OK, item)

    }

    post("/class-promotions") {
        val req = call.receive<CreateClassPromotionRequest>()
        call.respond(HttpStatusCode.Created, repo.create(req))
    }

    patch("/class-promotions/{id}") {
        val id = call.parameters["id"]!!.toInt()
        val req = call.receive<PatchClassPromotionRequest>()
        val updated = repo.patch(id, req) ?: return@patch call.respond(HttpStatusCode.NotFound)
        call.respond(HttpStatusCode.OK, updated)

    }

    delete("/class-promotions/{id}") {
        val id = call.parameters["id"]!!.toInt()
        val ok = repo.delete(id)
        if (!ok) return@delete call.respond(HttpStatusCode.NotFound)
        call.respond(HttpStatusCode.NoContent)
    }

}