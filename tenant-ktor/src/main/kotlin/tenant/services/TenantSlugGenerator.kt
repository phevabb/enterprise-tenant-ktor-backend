package com.example.tenant.services



import java.security.SecureRandom

object TenantSlugGenerator {
    private val random = SecureRandom()
    private val alphabet = "abcdefghijklmnopqrstuvwxyz0123456789"

    fun generate(length: Int = 8): String {
        val slug = buildString {
            repeat(length) {
                append(alphabet[random.nextInt(alphabet.length)])
            }
        }
        return "t-$slug"
    }
}