package sms.services


import com.example.academics.repos.setTenantSchema
import com.example.tenant.tables.TenantsTable
import org.jetbrains.exposed.sql.ResultRow

import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import sms.dto.SmsWalletResponse
import sms.tables.SmsWalletsTable
import java.math.BigDecimal
import java.time.Instant

object SmsWalletClientService {

    fun getOrCreateWalletByTenantCode(
        tenantCode: String
    ): SmsWalletResponse {

        val normalizedTenantCode =
            normalizeTenantCode(
                tenantCode
            )

        require(
            normalizedTenantCode.isNotBlank()
        ) {
            "Tenant code is required."
        }

        val now =
            Instant.now().toString()

        return transaction {

            setTenantSchema(
                "public"
            )

            val existingWallet =
                SmsWalletsTable
                    .selectAll()
                    .where {
                        SmsWalletsTable.tenantCode eq normalizedTenantCode
                    }
                    .limit(1)
                    .singleOrNull()

            if (existingWallet != null) {
                return@transaction existingWallet.toWalletResponse()
            }

            val tenantRow =
                TenantsTable
                    .selectAll()
                    .where {
                        TenantsTable.tenantCode eq normalizedTenantCode
                    }
                    .limit(1)
                    .singleOrNull()
                    ?: throw IllegalArgumentException(
                        "Tenant not found for code: $normalizedTenantCode"
                    )

            val schoolName =
                tenantRow[TenantsTable.schoolName]

            val insertedId =
                SmsWalletsTable.insert {

                    it[SmsWalletsTable.tenantCode] =
                        normalizedTenantCode

                    it[SmsWalletsTable.schoolName] =
                        schoolName

                    it[cashBalance] =
                        BigDecimal("0.00")

                    it[smsBalance] =
                        0

                    it[totalCashLoaded] =
                        BigDecimal("0.00")

                    it[totalSmsPurchased] =
                        0

                    it[totalSmsUsed] =
                        0

                    it[status] =
                        "active"

                    it[createdAt] =
                        now

                    it[updatedAt] =
                        now

                } get SmsWalletsTable.id

            SmsWalletsTable
                .selectAll()
                .where {
                    SmsWalletsTable.id eq insertedId.value
                }
                .single()
                .toWalletResponse()
        }
    }

    private fun ResultRow.toWalletResponse(): SmsWalletResponse {

        return SmsWalletResponse(
            id = this[SmsWalletsTable.id].value,
            tenantCode = this[SmsWalletsTable.tenantCode],
            schoolName = this[SmsWalletsTable.schoolName],
            cashBalance = this[SmsWalletsTable.cashBalance].toPlainString(),
            smsBalance = this[SmsWalletsTable.smsBalance],
            totalCashLoaded = this[SmsWalletsTable.totalCashLoaded].toPlainString(),
            totalSmsPurchased = this[SmsWalletsTable.totalSmsPurchased],
            totalSmsUsed = this[SmsWalletsTable.totalSmsUsed],
            status = this[SmsWalletsTable.status],
            createdAt = this[SmsWalletsTable.createdAt],
            updatedAt = this[SmsWalletsTable.updatedAt]
        )
    }

    private fun normalizeTenantCode(
        tenantCode: String
    ): String {

        return tenantCode
            .trim()
            .lowercase()
            .replace(
                Regex("[^a-z0-9_]"),
                ""
            )
    }
}