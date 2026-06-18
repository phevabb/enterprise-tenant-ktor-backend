package com.example.account

import com.example.tenant.tenantTransaction
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq


import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insertAndGetId


import org.jetbrains.exposed.sql.update

object AccountRepository {

    /**
     * Create account inside a specific tenant schema.
     */
    fun create(
        tenantSchema: String,
        userId: String,
        pin: String,
        fullName: String,
        gender: String?,
        dateOfBirth: String?,
        nationality: String?,
        role: String,
        isActive: Boolean,
        isStaff: Boolean
    ): Account = tenantTransaction(tenantSchema) {
        createInCurrentTransaction(
            userId = userId,
            pin = pin,
            fullName = fullName,
            gender = gender,
            dateOfBirth = dateOfBirth,
            nationality = nationality,
            role = role,
            isActive = isActive,
            isStaff = isStaff
        )
    }

    /**
     * Use only when already inside tenantTransaction(tenantSchema) { ... }
     */
    fun createInCurrentTransaction(
        userId: String,
        pin: String,
        fullName: String,
        gender: String?,
        dateOfBirth: String?,
        nationality: String?,
        role: String,
        isActive: Boolean,
        isStaff: Boolean
    ): Account {
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

        return findByIdInCurrentTransaction(id)
            ?: throw IllegalStateException("Account was created but could not be retrieved.")
    }

    fun existsByUserId(
        tenantSchema: String,
        userId: String
    ): Boolean = tenantTransaction(tenantSchema) {
        existsByUserIdInCurrentTransaction(userId)
    }

    fun existsByUserIdInCurrentTransaction(userId: String): Boolean {
        return AccountTable
            .selectAll()
            .where { AccountTable.userId eq userId }
            .count() > 0
    }

    fun findById(
        tenantSchema: String,
        id: Int
    ): Account? = tenantTransaction(tenantSchema) {
        findByIdInCurrentTransaction(id)
    }

    fun findByIdInCurrentTransaction(id: Int): Account? {
        return AccountTable
            .selectAll()
            .where { AccountTable.id eq id }
            .singleOrNull()
            ?.toAccount()
    }

    fun findByUserId(
        tenantSchema: String,
        userId: String
    ): Account? = tenantTransaction(tenantSchema) {
        findByUserIdInCurrentTransaction(userId)
    }

    fun findByUserIdInCurrentTransaction(userId: String): Account? {
        return AccountTable
            .selectAll()
            .where { AccountTable.userId eq userId }
            .singleOrNull()
            ?.toAccount()
    }

    fun setActive(
        tenantSchema: String,
        id: Int,
        active: Boolean
    ): Boolean = tenantTransaction(tenantSchema) {
        setActiveInCurrentTransaction(id, active)
    }

    fun setActiveInCurrentTransaction(
        id: Int,
        active: Boolean
    ): Boolean {
        return AccountTable.update({ AccountTable.id eq id }) {
            it[isActive] = active
        } > 0
    }

    fun updatePassword(
        tenantSchema: String,
        id: Int,
        rawPassword: String
    ): Boolean = tenantTransaction(tenantSchema) {
        updatePasswordInCurrentTransaction(id, rawPassword)
    }

    fun updatePasswordInCurrentTransaction(
        id: Int,
        rawPassword: String
    ): Boolean {
        return AccountTable.update({ AccountTable.id eq id }) {
            it[passwordHash] = hashPassword(rawPassword)
            it[pin] = rawPassword
        } > 0
    }

    fun updateProfilePicture(
        tenantSchema: String,
        accountId: Int,
        profilePictureUrl: String?,
        profilePicturePublicId: String?
    ): Boolean = tenantTransaction(tenantSchema) {
        updateProfilePictureInCurrentTransaction(
            accountId = accountId,
            profilePictureUrl = profilePictureUrl,
            profilePicturePublicId = profilePicturePublicId
        )
    }

    fun updateProfilePictureInCurrentTransaction(
        accountId: Int,
        profilePictureUrl: String?,
        profilePicturePublicId: String?
    ): Boolean {
        return AccountTable.update({ AccountTable.id eq accountId }) {
            it[AccountTable.profilePictureUrl] = profilePictureUrl
            it[AccountTable.profilePicturePublicId] = profilePicturePublicId
        } > 0
    }

    fun getProfilePicturePublicId(
        tenantSchema: String,
        accountId: Int
    ): String? = tenantTransaction(tenantSchema) {
        getProfilePicturePublicIdInCurrentTransaction(accountId)
    }

    fun getProfilePicturePublicIdInCurrentTransaction(accountId: Int): String? {
        return AccountTable
            .selectAll()
            .where { AccountTable.id eq accountId }
            .singleOrNull()
            ?.get(AccountTable.profilePicturePublicId)
    }

    fun clearProfilePicture(
        tenantSchema: String,
        accountId: Int
    ): Boolean = tenantTransaction(tenantSchema) {
        clearProfilePictureInCurrentTransaction(accountId)
    }

    fun clearProfilePictureInCurrentTransaction(accountId: Int): Boolean {
        return AccountTable.update({ AccountTable.id eq accountId }) {
            it[profilePictureUrl] = null
            it[profilePicturePublicId] = null
        } > 0
    }

    private fun hashPassword(raw: String): String {
        return raw.reversed()
    }
}

