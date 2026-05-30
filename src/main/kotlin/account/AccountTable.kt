package com.example.account

import org.jetbrains.exposed.dao.id.IntIdTable

enum class Role { student, staff, principal, administrator }
enum class Gender { male, female }

object AccountTable : IntIdTable("accounts") {
    val userId = varchar("user_id", 20).uniqueIndex()
    val pin = varchar("pin", 20)
    val fullName = varchar("full_name", 222)
    val gender = varchar("gender", 10).nullable()
    val dateOfBirth = varchar("date_of_birth", 20).nullable()
    val nationality = varchar("nationality", 100).nullable()
    val role = varchar("role", 20)
    val isActive = bool("is_active").default(true)
    val isStaff = bool("is_staff").default(false)
    val passwordHash = varchar("password_hash", 255)
    val profilePictureUrl = varchar("profile_picture_url", 500).nullable()
    val profilePicturePublicId = varchar("profile_picture_public_id", 255).nullable()
}

