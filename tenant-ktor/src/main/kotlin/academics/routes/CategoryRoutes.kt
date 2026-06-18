package com.example.academics.routes



import com.example.academics.repos.CategoryRepository
import com.example.tenant.currentTenant
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
            val tenant = call.currentTenant()
            println("tenant is $tenant")
            println("really got hereeeeeeeeee")

            val data = CategoryRepository.findAll(
                tenantSchema = tenant.tenantSchema
            )

            call.respond(HttpStatusCode.OK, data)
        }

        post {
            val req = call.receive<CategoryNameRequest>()

            if (req.name.isBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Name is required")
                )
                return@post
            }

            val tenant = call.currentTenant()

            val created = CategoryRepository.create(
                tenantSchema = tenant.tenantSchema,
                name = req.name.trim()
            )

            call.respond(HttpStatusCode.Created, created)
        }

        put("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()

            if (id == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Invalid id")
                )
                return@put
            }

            val req = call.receive<CategoryNameRequest>()

            if (req.name.isBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Name is required")
                )
                return@put
            }

            val tenant = call.currentTenant()

            val updated = CategoryRepository.updateName(
                tenantSchema = tenant.tenantSchema,
                id = id,
                name = req.name.trim()
            )

            if (updated == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Category not found")
                )
            } else {
                call.respond(HttpStatusCode.OK, updated)
            }
        }

        delete("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()

            if (id == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Invalid id")
                )
                return@delete
            }

            val tenant = call.currentTenant()

            val ok = CategoryRepository.deleteIfUnused(
                tenantSchema = tenant.tenantSchema,
                id = id
            )

            if (!ok) {
                call.respond(
                    HttpStatusCode.Conflict,
                    mapOf(
                        "error" to
                                "Cannot delete category: it is used by classes or subjects"
                    )
                )
            } else {
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}