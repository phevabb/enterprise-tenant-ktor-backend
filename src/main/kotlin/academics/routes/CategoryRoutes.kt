package com.example.academics.routes



import com.example.academics.repos.CategoryRepository
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class CategoryNameRequest(val name: String)

fun Route.categoryRoutes() {

    authenticate("auth-jwt") {

        // ✅ GET /api/categories  (returns category + children)
        get {
            val data = CategoryRepository.findAll()
            call.respond(HttpStatusCode.OK, data)
        }

        // ✅ POST /api/categories  (create category name only)
        post {
            val req = call.receive<CategoryNameRequest>()
            if (req.name.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Name is required"))
                return@post
            }

            val created = CategoryRepository.create(req.name.trim())
            call.respond(HttpStatusCode.Created, created)
        }

        // ✅ PUT /api/categories/{id}  (rename category only)
        put("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid id"))
                return@put
            }

            val req = call.receive<CategoryNameRequest>()
            if (req.name.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Name is required"))
                return@put
            }

            val updated = CategoryRepository.updateName(id, req.name.trim())
            if (updated == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Category not found"))
            } else {
                call.respond(HttpStatusCode.OK, updated)
            }
        }

        // ✅ DELETE /api/categories/{id}
        delete("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid id"))
                return@delete
            }

            val ok = CategoryRepository.deleteIfUnused(id)
            if (!ok) {
                call.respond(
                    HttpStatusCode.Conflict,
                    mapOf("error" to "Cannot delete category: it is used by classes or subjects")
                )
            } else {
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}