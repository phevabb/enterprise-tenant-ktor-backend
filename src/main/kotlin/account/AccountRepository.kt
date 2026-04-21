package com.example.account

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

object AccountRepository {

    /** ✅ Create account (Django: User.objects.create + set_password) */
    fun create(
        userId: String,
        pin: String,
        fullName: String,
        gender: String?,
        dateOfBirth: String?,
        nationality: String?,
        role: String,
        isActive: Boolean,
        isStaff: Boolean
    ): Account = transaction {

        val id = AccountTable.insertAndGetId {
            it[AccountTable.userId] = userId
            it[AccountTable.pin] = pin
            it[AccountTable.fullName] = fullName
            it[AccountTable.gender] = gender
            it[AccountTable.dateOfBirth] = dateOfBirth
            it[AccountTable.nationality] = nationality
            it[AccountTable.role] = role
            it[AccountTable.isActive] = isActive
            it[AccountTable.isStaff] = isStaff
            it[AccountTable.passwordHash] = hashPassword(pin)
        }.value

        findById(id)!!
    }

    /** ✅ Check if a user_id exists */
    fun existsByUserId(userId: String): Boolean = transaction {
        AccountTable
            .selectAll()
            .where { AccountTable.userId eq userId }
            .count() > 0
    }

    /** ✅ Find by internal DB id */
    fun findById(id: Int): Account? = transaction {
        AccountTable
            .selectAll()
            .where { AccountTable.id eq id }
            .singleOrNull()
            ?.toAccount()
    }

    /** ✅ Find by public user_id */
    fun findByUserId(userId: String): Account? = transaction {
        AccountTable
            .selectAll()
            .where { AccountTable.userId eq userId }
            .singleOrNull()
            ?.toAccount()
    }

    /** ✅ Enable / disable account */
    fun setActive(id: Int, active: Boolean): Boolean = transaction {
        AccountTable.update({ AccountTable.id eq id }) {
            it[isActive] = active
        } > 0
    }

    /** ✅ Update password / pin */
    fun updatePassword(id: Int, rawPassword: String): Boolean = transaction {
        AccountTable.update({ AccountTable.id eq id }) {
            it[passwordHash] = hashPassword(rawPassword)
            it[pin] = rawPassword
        } > 0
    }

    /** ⚠️ Replace with BCrypt/Argon2 later */
    private fun hashPassword(raw: String): String {
        return raw.reversed() // placeholder
    }
}