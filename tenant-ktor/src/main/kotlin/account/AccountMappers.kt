package com.example.account

import org.jetbrains.exposed.sql.ResultRow
import java.time.LocalDate

private fun roleFromDb(value: String): Role {
    return when (value.trim().lowercase()) {
        "student" -> Role.student
        "staff" -> Role.staff
        "principal" -> Role.principal
        "administrator" -> Role.administrator

        // Optional fallback for old/failed records saved as "admin"
        "admin" -> Role.administrator

        else -> throw IllegalArgumentException("Unknown account role from database: '$value'")
    }
}

private fun genderFromDb(value: String): Gender? {
    return when (value.trim().lowercase()) {
        "" -> null
        "male" -> Gender.male
        "female" -> Gender.female
        else -> throw IllegalArgumentException("Unknown gender from database: '$value'")
    }
}

private fun parseDateOfBirth(value: String?): LocalDate? {
    if (value.isNullOrBlank()) return null

    return try {
        LocalDate.parse(value)
    } catch (e: Exception) {
        null
    }
}

fun ResultRow.toAccount(): Account = Account(
    id = this[AccountTable.id].value,
    userId = this[AccountTable.userId],
    role = roleFromDb(this[AccountTable.role]),
    pin = this[AccountTable.pin],
    fullName = this[AccountTable.fullName],
    isActive = this[AccountTable.isActive],
    isStaff = this[AccountTable.isStaff],

    gender = this[AccountTable.gender]?.let { genderFromDb(it) },

    dateOfBirth = parseDateOfBirth(this[AccountTable.dateOfBirth]),

    createdAt = null,

    nationality = this[AccountTable.nationality],
    email = null,
    profilePictureUrl = this[AccountTable.profilePictureUrl]
)