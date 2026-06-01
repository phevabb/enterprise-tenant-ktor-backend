package com.example.principal.repos


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

object PrincipalRepository {


    fun create(userId: Int?): PrincipalProfileResponse = transaction {

        val id = PrincipalTable.insertAndGetId {
            it[user] = userId?.let { EntityID(it, AccountTable) }
        }.value

        findByIdWithUser(id)!!
    }


    fun findAllWithUser(search: String?): List<PrincipalProfileResponse> = transaction {

        val query = PrincipalTable
            .join(AccountTable, JoinType.INNER, PrincipalTable.user, AccountTable.id)
            .selectAll()

        if (!search.isNullOrBlank()) {
            val q = "%${search.lowercase()}%"
            query.andWhere {
                AccountTable.fullName.lowerCase() like q
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

            PrincipalProfileResponse(
                id = row[PrincipalTable.id].value,
                user = user
            )
        }
    }


    fun findByIdWithUser(id: Int): PrincipalProfileResponse? = transaction {

        PrincipalTable
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


    fun patchNested(id: Int, req: PatchPrincipalRequest): PrincipalProfileResponse? = transaction {

        val row = PrincipalTable
            .selectAll()
            .where { PrincipalTable.id eq id }
            .singleOrNull()
            ?: return@transaction null

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

        findByIdWithUser(id)
    }


    fun delete(id: Int): Boolean = transaction {
        PrincipalTable.deleteWhere { PrincipalTable.id eq id } > 0
    }


    fun countStaff(): Int = transaction {
        StaffTable.selectAll().count().toInt()
    }


    fun countAdmins(): Int = transaction {
        AdminTable.selectAll().count().toInt()
    }


}