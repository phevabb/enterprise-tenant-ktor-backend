package com.example.auth

import com.example.account.AccountRepository
import com.example.account.Role
import io.ktor.http.*
import io.ktor.server.auth.authenticate
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val userId: String,
    val role: String,   // ✅ ADD THIS (you said you want role included)
    val pin: String
)

@Serializable
data class AuthUserResponse(
    val id: Int,
    val userId: String,
    val fullName: String,
    val role: String,
    val isActive: Boolean,
    val isStaff: Boolean
)

@Serializable
data class LoginResponse(
    val access: String,
    val role: String,
    val user: AuthUserResponse
)

/**
 * Convert role string from client -> Role enum safely.
 * Returns null if role is invalid.
 */
private fun roleFromInputOrNull(role: String): Role? =
    Role.entries.firstOrNull { it.name.equals(role.trim(), ignoreCase = true) }

fun Route.authRoutes() {

    post("/login") {

        val req = call.receive<LoginRequest>()

        val user = AccountRepository.findByUserId(req.userId)

        if (user == null) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid credentials"))
            return@post
        }

        // ✅ Validate PIN
        if (user.pin != req.pin) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid credentials"))
            return@post
        }

        // ✅ Validate ROLE (client role must match user's stored role)
        val requestedRole = roleFromInputOrNull(req.role)
        if (requestedRole == null) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid role"))
            return@post
        }

        // If your user.role is Role enum (your earlier screenshot indicates this):
        if (user.role != requestedRole) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Role mismatch"))
            return@post
        }

        // ✅ Generate token using Role enum
        val token = JwtConfig.generateToken(user.id, user.role)

        val userDto = AuthUserResponse(
            id = user.id,
            userId = user.userId,
            fullName = user.fullName,
            role = user.role.name,
            isActive = user.isActive,
            isStaff = user.isStaff
        )

        call.respond(
            status = HttpStatusCode.OK,
            message = LoginResponse(
                access = token,
                role = user.role.name,   // ✅ FIX: provide role param
                user = userDto
            )
        )
    }

    authenticate("auth-jwt") {
        post("/logout") {
            call.respond(HttpStatusCode.OK, mapOf("message" to "Logged out"))
        }
    }
}

