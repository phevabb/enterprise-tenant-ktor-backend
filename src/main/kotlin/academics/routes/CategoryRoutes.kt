package com.example.academics.routes

import com.example.academics.repos.CategoryRepository



import com.example.student.repos.NewGradeClassRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class CreateCategoryRequest(
    val name: String
)

@Serializable
data class AssignClassesRequest(
    val classIds: List<Int>
)

fun Route.categoryRoutes() {

    // ✅ GET ALL CATEGORIES (with classes inside)
    get {
        val categories = CategoryRepository.findAll()
        call.respond(HttpStatusCode.OK, categories)
    }

    // ✅ CREATE CATEGORY
    post {
        val req = call.receive<CreateCategoryRequest>()

        if (req.name.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Name is required"))
            return@post
        }

        val created = CategoryRepository.create(req.name)
        call.respond(HttpStatusCode.Created, created)
    }

    // ✅ ASSIGN MULTIPLE CLASSES TO CATEGORY
    put("{id}/classes") {

        val categoryId = call.parameters["id"]?.toIntOrNull()
        if (categoryId == null) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid category id"))
            return@put
        }

        val req = call.receive<AssignClassesRequest>()

        if (req.classIds.isEmpty()) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "classIds required"))
            return@put
        }

        // ✅ assign many classes
        NewGradeClassRepository.assignManyToCategory(req.classIds, categoryId)

        call.respond(HttpStatusCode.OK, mapOf("message" to "Classes assigned"))
    }

    // ✅ DELETE CATEGORY
    delete("{id}") {

        val id = call.parameters["id"]?.toIntOrNull()
        if (id == null) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid id"))
            return@delete
        }

        val ok = CategoryRepository.delete(id)

        if (!ok) {
            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Not found"))
        } else {
            call.respond(HttpStatusCode.NoContent)
        }
    }
}