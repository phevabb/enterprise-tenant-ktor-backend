package com.example.admin.repos

import com.example.account.AccountTable
import com.example.admin.dtos.requests.PatchAdminRequest
import com.example.admin.dtos.response.AdminProfileResponse
import com.example.admin.mappers.toAdminProfile
import com.example.admin.models.AdminProfile
import com.example.admin.tables.AdminTable
import com.example.student.dtos.response.StudentUserResponse
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like

object AdminRepository {

    fun createInCurrentTransaction(profile: AdminProfile): AdminProfileResponse {
        val id = AdminTable.insertAndGetId {
            it[user] = profile.user?.let { userId ->
                EntityID(userId, AccountTable)
            }
            it[tel] = profile.tel
        }.value

        return findByIdWithUserAndClassInCurrentTransaction(id)
            ?: throw IllegalStateException("Admin profile was created but could not be retrieved.")
    }

    fun create(profile: AdminProfile): AdminProfileResponse = transaction {
        createInCurrentTransaction(profile)
    }

    fun findAllInCurrentTransaction(): List<AdminProfile> {
        return AdminTable
            .selectAll()
            .orderBy(AdminTable.id, SortOrder.DESC)
            .map { it.toAdminProfile() }
    }

    fun findAll(): List<AdminProfile> = transaction {
        findAllInCurrentTransaction()
    }

    fun findByIdInCurrentTransaction(id: Int): AdminProfile? {
        return AdminTable
            .selectAll()
            .where { AdminTable.id eq id }
            .singleOrNull()
            ?.toAdminProfile()
    }

    fun findById(id: Int): AdminProfile? = transaction {
        findByIdInCurrentTransaction(id)
    }

    fun findByUserIdInCurrentTransaction(userId: Int): AdminProfile? {
        return AdminTable
            .selectAll()
            .where { AdminTable.user eq EntityID(userId, AccountTable) }
            .singleOrNull()
            ?.toAdminProfile()
    }

    fun findByUserId(userId: Int): AdminProfile? = transaction {
        findByUserIdInCurrentTransaction(userId)
    }

    fun deleteInCurrentTransaction(id: Int): Boolean {
        return AdminTable.deleteWhere { AdminTable.id eq id } > 0
    }

    fun delete(id: Int): Boolean = transaction {
        deleteInCurrentTransaction(id)
    }

    fun existsByIdInCurrentTransaction(id: Int): Boolean {
        return AdminTable
            .selectAll()
            .where { AdminTable.id eq id }
            .count() > 0
    }

    fun existsById(id: Int): Boolean = transaction {
        existsByIdInCurrentTransaction(id)
    }

    fun findAllWithUserAndClassInCurrentTransaction(
        search: String?
    ): List<AdminProfileResponse> {
        val query = AdminTable
            .join(AccountTable, JoinType.INNER, AdminTable.user, AccountTable.id)
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

            AdminProfileResponse(
                id = row[AdminTable.id].value,
                user = user,
                tel = row[AdminTable.tel]
            )
        }
    }

    fun findAllWithUserAndClass(search: String?): List<AdminProfileResponse> = transaction {
        findAllWithUserAndClassInCurrentTransaction(search)
    }

    fun findByIdWithUserAndClassInCurrentTransaction(id: Int): AdminProfileResponse? {
        val query = AdminTable
            .join(AccountTable, JoinType.INNER, AdminTable.user, AccountTable.id)

        return query
            .selectAll()
            .where { AdminTable.id eq id }
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

                AdminProfileResponse(
                    id = row[AdminTable.id].value,
                    user = user,
                    tel = row[AdminTable.tel]
                )
            }
    }

    fun findByIdWithUserAndClass(id: Int): AdminProfileResponse? = transaction {
        findByIdWithUserAndClassInCurrentTransaction(id)
    }

    fun patchNestedInCurrentTransaction(
        id: Int,
        req: PatchAdminRequest
    ): AdminProfileResponse? {
        val row = AdminTable
            .selectAll()
            .where { AdminTable.id eq id }
            .singleOrNull()
            ?: return null

        val accountId = row[AdminTable.user]?.value

        val hasAdminFieldsToUpdate = req.tel != null

        if (hasAdminFieldsToUpdate) {
            AdminTable.update({ AdminTable.id eq id }) { u ->
                req.tel?.let { u[AdminTable.tel] = it }
            }
        }

        if (accountId != null) {
            req.user?.let { userPatch ->
                val hasUserFieldsToUpdate =
                    userPatch.fullName != null ||
                            userPatch.gender != null ||
                            userPatch.dateOfBirth != null ||
                            userPatch.nationality != null ||
                            userPatch.role != null ||
                            userPatch.isActive != null ||
                            userPatch.isStaff != null

                if (hasUserFieldsToUpdate) {
                    AccountTable.update({ AccountTable.id eq accountId }) { a ->
                        userPatch.fullName?.let { a[AccountTable.fullName] = it }
                        userPatch.gender?.let { a[AccountTable.gender] = it }
                        userPatch.dateOfBirth?.let { a[AccountTable.dateOfBirth] = it }
                        userPatch.nationality?.let { a[AccountTable.nationality] = it }
                        userPatch.role?.let { a[AccountTable.role] = it.lowercase() }
                        userPatch.isActive?.let { a[AccountTable.isActive] = it }
                        userPatch.isStaff?.let { a[AccountTable.isStaff] = it }
                    }
                }
            }
        }

        return findByIdWithUserAndClassInCurrentTransaction(id)
    }

    fun patchNested(id: Int, req: PatchAdminRequest): AdminProfileResponse? = transaction {
        patchNestedInCurrentTransaction(id, req)
    }
}