package com.example.admin.repos



import com.example.account.AccountTable
import com.example.admin.dtos.requests.PatchAdminRequest
import com.example.admin.dtos.response.AdminProfileResponse
import com.example.admin.mappers.toAdminProfile
import com.example.admin.models.AdminProfile
import com.example.admin.tables.AdminTable

import com.example.student.dtos.response.StudentUserResponse


import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId

object AdminRepository {

    /** ✅ Create Admin profile */
    fun create(profile: AdminProfile) = transaction {

        val id = AdminTable.insertAndGetId {

            it[user] = profile.user?.let {
                EntityID(it, AccountTable)
            }


            it[tel] = profile.tel

        }.value

        findByIdWithUserAndClass(id)!!
    }

    /** ✅ Get all Admin */
    fun findAll(): List<AdminProfile> = transaction {
        AdminTable
            .selectAll()
            .orderBy(AdminTable.id, SortOrder.DESC)
            .map { it.toAdminProfile() }
    }

    /** ✅ Get by profile ID */
    fun findById(id: Int): AdminProfile? = transaction {
        AdminTable
            .selectAll()
            .where { AdminTable.id eq id }
            .singleOrNull()
            ?.toAdminProfile()
    }

    /** ✅ OneToOne lookup */
    fun findByUserId(userId: Int): AdminProfile? = transaction {
        AdminTable
            .selectAll()
            .where { AdminTable.user eq EntityID(userId, AccountTable) }
            .singleOrNull()
            ?.toAdminProfile()
    }

    /** ✅ Delete */
    fun delete(id: Int): Boolean = transaction {
        AdminTable.deleteWhere { AdminTable.id eq id } > 0
    }

    /** ✅ Exists */
    fun existsById(id: Int): Boolean = transaction {
        AdminTable
            .selectAll()
            .where { AdminTable.id eq id }
            .count() > 0
    }

    /**
     * ✅ JOIN → Return nested response (LIKE Django serializer)
     */
    fun findAllWithUserAndClass(search: String?): List<AdminProfileResponse> = transaction {

        val query = AdminTable
            .join(AccountTable, JoinType.INNER, AdminTable.user, AccountTable.id)
            .selectAll()

        if (!search.isNullOrBlank()) {
            val q = "%${search.lowercase()}%"
            query.andWhere {
                (AccountTable.fullName.lowerCase() like q)

            }
        }

        query.map { row ->

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

    /** ✅ Single with JOIN (like Django detail view) */
    fun findByIdWithUserAndClass(id: Int): AdminProfileResponse? = transaction {

        val query = AdminTable
            .join(AccountTable, JoinType.INNER, AdminTable.user, AccountTable.id)

        query
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

    fun patchNested(id: Int, req: PatchAdminRequest): AdminProfileResponse? = transaction {

        val row = AdminTable
            .selectAll()
            .where { AdminTable.id eq id }
            .singleOrNull()
            ?: return@transaction null

        val accountId = row[AdminTable.user]?.value

        // ✅ update Admin fields
        AdminTable.update({ AdminTable.id eq id }) { u ->


            req.tel?.let { u[AdminTable.tel] = it }
        }

        // ✅ update user (if present)
        if (accountId != null) {
            req.user?.let { userPatch ->
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

        // ✅ return updated data
        findByIdWithUserAndClass(id)
    }
}