package com.example.account

import org.jetbrains.exposed.sql.ResultRow
import java.time.Instant
import java.time.LocalDate

private fun roleFromDb(value: String): Role =
    Role.entries.first { it.name.equals(value, ignoreCase = true) }

private fun genderFromDb(value: String): Gender =
    Gender.entries.first { it.name.equals(value, ignoreCase = true) }

fun ResultRow.toAccount(): Account = Account(
    id = this[AccountTable.id].value,
    userId = this[AccountTable.userId],
    role = roleFromDb(this[AccountTable.role]),
    pin = this[AccountTable.pin],
    fullName = this[AccountTable.fullName],
    isActive = this[AccountTable.isActive],
    isStaff = this[AccountTable.isStaff],
    gender = this[AccountTable.gender]?.let { genderFromDb(it) },

    // Your table currently stores dateOfBirth as VARCHAR; parse it if present.
    dateOfBirth = this[AccountTable.dateOfBirth]?.let(LocalDate::parse),

    // Your table does NOT have createdAt yet; keep it null for now.
    createdAt = null,

    nationality = this[AccountTable.nationality],
    email = null,
    profilePictureUrl = null
)