package com.example.principal.repos


import com.example.academics.repos.setTenantSchema
import com.example.account.AccountTable
import com.example.admin.tables.AdminTable
import com.example.principal.dtos.requests.PatchPrincipalRequest
import com.example.principal.dtos.responses.PrincipalProfileResponse

import com.example.principal.tables.PrincipalTable
import com.example.staff.tables.StaffTable
import com.example.student.dtos.response.StudentUserResponse

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq


import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like

object PrincipalRepository {

    /**
     * Use this when you are already inside the correct tenant transaction/schema.
     * Example:
     *   tenantTransaction(tenantSchema) {
     *       PrincipalRepository.createInCurrentTransaction(accountId)
     *   }
     */
    fun createInCurrentTransaction(userId: Int): PrincipalProfileResponse {
        val id = PrincipalTable.insertAndGetId {
            it[user] = EntityID(userId, AccountTable)
        }.value

        return findByIdWithUserInCurrentTransaction(id)
            ?: throw IllegalStateException("Principal profile was created but could not be retrieved.")
    }

    /**
     * Wrapper version if you want the repository to open a transaction itself.
     * For tenant-aware flows, prefer createInCurrentTransaction(...)
     */
    fun create(tenantSchema: String,  userId: Int): PrincipalProfileResponse = transaction {
        setTenantSchema(tenantSchema)
        createInCurrentTransaction(userId)
    }

    /**
     * Use this when you are already inside the correct tenant transaction/schema.
     */
    fun findByIdWithUserInCurrentTransaction(id: Int): PrincipalProfileResponse? {
        return PrincipalTable
            .join(AccountTable, JoinType.INNER, PrincipalTable.user, AccountTable.id)
            .selectAll()
            .where { PrincipalTable.id eq id }
            .singleOrNull()
            ?.let { row ->
                val user = StudentUserResponse(
                    id = row[AccountTable.id].value,
                    userId = row[AccountTable.userId],
                    fullName = row[AccountTable.fullName],
                    gender = row[AccountTable.gender],
                    role = row[AccountTable.role],
                    isActive = row[AccountTable.isActive],
                    pin = row[AccountTable.pin],
                    dateOfBirth = row[AccountTable.dateOfBirth]
                )

                PrincipalProfileResponse(
                    id = row[PrincipalTable.id].value,
                    user = user
                )
            }
    }

    /**
     * Wrapper version if you want the repository to open a transaction itself.
     */
    fun findByIdWithUser(tenantSchema: String, id: Int): PrincipalProfileResponse? = transaction {
        setTenantSchema(tenantSchema)
        findByIdWithUserInCurrentTransaction(id)
    }

    /**
     * Use this when you are already inside the correct tenant transaction/schema.
     */
    fun findAllWithUserInCurrentTransaction(search: String?): List<PrincipalProfileResponse> {
        val query = PrincipalTable
            .join(AccountTable, JoinType.INNER, PrincipalTable.user, AccountTable.id)
            .selectAll()

        if (!search.isNullOrBlank()) {
            val q = "%${search.lowercase()}%"
            query.andWhere {
                AccountTable.fullName.lowerCase() like q
            }
        }

        return query.map { row ->
            val user = StudentUserResponse(
                id = row[AccountTable.id].value,
                userId = row[AccountTable.userId],
                fullName = row[AccountTable.fullName],
                gender = row[AccountTable.gender],
                role = row[AccountTable.role],
                isActive = row[AccountTable.isActive],
                pin = row[AccountTable.pin],
                dateOfBirth = row[AccountTable.dateOfBirth]
            )

            PrincipalProfileResponse(
                id = row[PrincipalTable.id].value,
                user = user
            )
        }
    }

    /**
     * Wrapper version if you want the repository to open a transaction itself.
     */
    fun findAllWithUser(tenantSchema: String, search: String?): List<PrincipalProfileResponse> = transaction {
        setTenantSchema(tenantSchema)
        findAllWithUserInCurrentTransaction(search)
    }

    /**
     * Use this when you are already inside the correct tenant transaction/schema.
     */
    fun patchNestedInCurrentTransaction(
        id: Int,
        req: PatchPrincipalRequest
    ): PrincipalProfileResponse? {
        val row = PrincipalTable
            .selectAll()
            .where { PrincipalTable.id eq id }
            .singleOrNull()
            ?: return null

        val accountId = row[PrincipalTable.user]?.value

        if (accountId != null) {
            req.user?.let { patch ->
                AccountTable.update({ AccountTable.id eq accountId }) { a ->
                    patch.fullName?.let { a[AccountTable.fullName] = it }
                    patch.gender?.let { a[AccountTable.gender] = it }
                    patch.dateOfBirth?.let { a[AccountTable.dateOfBirth] = it }
                    patch.nationality?.let { a[AccountTable.nationality] = it }
                    patch.role?.let { a[AccountTable.role] = it.lowercase() }
                    patch.isActive?.let { a[AccountTable.isActive] = it }
                    patch.isStaff?.let { a[AccountTable.isStaff] = it }
                }
            }
        }

        return findByIdWithUserInCurrentTransaction(id)
    }

    /**
     * Wrapper version if you want the repository to open a transaction itself.
     */
    fun patchNested(
        tenantSchema: String,
        id: Int,
        req: PatchPrincipalRequest
    ): PrincipalProfileResponse? = transaction {
        setTenantSchema(tenantSchema)
        patchNestedInCurrentTransaction(id, req)
    }

    /**
     * Use this when you are already inside the correct tenant transaction/schema.
     */
    fun deleteInCurrentTransaction(id: Int): Boolean {
        return PrincipalTable.deleteWhere { PrincipalTable.id eq id } > 0
    }

    /**
     * Wrapper version if you want the repository to open a transaction itself.
     */
    fun delete(tenantSchema: String,  id: Int): Boolean = transaction {
        setTenantSchema(tenantSchema)
        deleteInCurrentTransaction(id)
    }

    /**
     * Use this when you are already inside the correct tenant transaction/schema.
     */
    fun countStaffInCurrentTransaction(): Int {
        return StaffTable.selectAll().count().toInt()
    }

    /**
     * Wrapper version if you want the repository to open a transaction itself.
     */
    fun countStaff(tenantSchema: String): Int = transaction {
        setTenantSchema(tenantSchema)
        countStaffInCurrentTransaction()
    }

    /**
     * Use this when you are already inside the correct tenant transaction/schema.
     */
    fun countAdminsInCurrentTransaction(): Int {
        return AdminTable.selectAll().count().toInt()
    }

    /**
     * Wrapper version if you want the repository to open a transaction itself.
     */
    fun countAdmins(tenantSchema: String): Int = transaction {
        setTenantSchema(tenantSchema)
        countAdminsInCurrentTransaction()
    }
}

