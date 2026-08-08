package com.example.tenant.services


import com.example.account.AccountTable
import com.example.admin.tables.AdminTable
import com.example.principal.tables.PrincipalTable
import com.example.student.repos.setTenantSchema

import com.example.tenant.TenantResolver
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.transactions.transaction

object UpdateTenantPinsService {

    fun updatePins(
        tenantCode: String,
        adminPin: String?,
        principalPin: String?
    ) {

        val normalizedTenantCode = tenantCode
            .trim()
            .lowercase()
            .replace(Regex("[^a-z0-9_]"), "")

        println(
            "[PIN UPDATE] tenantCode=$tenantCode normalizedTenantCode=$normalizedTenantCode"
        )

        val tenant = TenantResolver()
            .resolveByTenantCode(normalizedTenantCode)
            ?: error(
                "Tenant not found for code: $normalizedTenantCode"
            )

        transaction {

            setTenantSchema(
                tenant.tenantSchema
            )

            adminPin?.let { pin ->

                println(
                    "[PIN UPDATE] Updating Administrator PIN"
                )

                val adminUserId = AdminTable
                    .selectAll()
                    .singleOrNull()
                    ?.get(AdminTable.user)
                    ?.value

                if (adminUserId != null) {

                    AccountTable.update(
                        {
                            AccountTable.id eq adminUserId
                        }
                    ) {
                        it[AccountTable.pin] = pin
                    }
                }
            }

            principalPin?.let { pin ->

                println(
                    "[PIN UPDATE] Updating Principal PIN"
                )

                val principalUserId = PrincipalTable
                    .selectAll()
                    .singleOrNull()
                    ?.get(PrincipalTable.user)
                    ?.value

                if (principalUserId != null) {

                    AccountTable.update(
                        {
                            AccountTable.id eq principalUserId
                        }
                    ) {
                        it[AccountTable.pin] = pin
                    }
                }
            }
        }

        println("[PIN UPDATE] Completed")
    }}