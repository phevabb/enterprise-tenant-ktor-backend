package com.example.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.account.Role
import java.util.Date

object JwtConfig {

    private const val SECRET = "super-secret"
    private const val ISSUER = "ktor-api"
    private const val AUDIENCE = "ktor-users"

    private val algorithm = Algorithm.HMAC256(SECRET)

    fun generateToken(userId: Int, role: Role): String {
        return JWT.create()
            .withAudience(AUDIENCE)
            .withIssuer(ISSUER)
            .withClaim("userId", userId)
            .withClaim("role", role.name) // ✅ enum -> string
            .withExpiresAt(Date(System.currentTimeMillis() + 86400000))
            .sign(algorithm)
    }

    fun verifier() = JWT
        .require(algorithm)
        .withAudience(AUDIENCE)
        .withIssuer(ISSUER)
        .build()
}