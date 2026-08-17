package superadmin.service


import com.example.academics.repos.setTenantSchema
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder

import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction


import sms.tables.SmsWalletTransactionsTable
import sms.tables.SmsWalletsTable
import superadmin.dto.response.SmsWalletResponse
import superadmin.dto.response.SmsWalletTransactionResponse

object SmsAdminWalletService {

    fun getAllWallets(): List<SmsWalletResponse> {

        return transaction {

            setTenantSchema("public")

            SmsWalletsTable
                .selectAll()
                .orderBy(
                    SmsWalletsTable.id,
                    SortOrder.DESC
                )
                .map { row ->
                    row.toWalletResponse()
                }
        }
    }

    fun getWalletByTenantCode(
        tenantCode: String
    ): SmsWalletResponse? {

        val normalizedTenantCode =
            normalizeTenantCode(
                tenantCode
            )

        return transaction {

            setTenantSchema("public")

            SmsWalletsTable
                .selectAll()
                .where {
                    SmsWalletsTable.tenantCode eq normalizedTenantCode
                }
                .singleOrNull()
                ?.toWalletResponse()
        }
    }

    fun getAllWalletTransactions(): List<SmsWalletTransactionResponse> {

        return transaction {

            setTenantSchema("public")

            SmsWalletTransactionsTable
                .selectAll()
                .orderBy(
                    SmsWalletTransactionsTable.id,
                    SortOrder.DESC
                )
                .map { row ->
                    row.toWalletTransactionResponse()
                }
        }
    }

    fun getWalletTransactionsByTenantCode(
        tenantCode: String
    ): List<SmsWalletTransactionResponse> {

        val normalizedTenantCode =
            normalizeTenantCode(
                tenantCode
            )

        return transaction {

            setTenantSchema("public")

            SmsWalletTransactionsTable
                .selectAll()
                .where {
                    SmsWalletTransactionsTable.tenantCode eq normalizedTenantCode
                }
                .orderBy(
                    SmsWalletTransactionsTable.id,
                    SortOrder.DESC
                )
                .map { row ->
                    row.toWalletTransactionResponse()
                }
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

    private fun ResultRow.toWalletTransactionResponse(): SmsWalletTransactionResponse {

        return SmsWalletTransactionResponse(
            id = this[SmsWalletTransactionsTable.id].value,
            tenantCode = this[SmsWalletTransactionsTable.tenantCode],
            type = this[SmsWalletTransactionsTable.type],
            amountCash = this[SmsWalletTransactionsTable.amountCash]
                ?.toPlainString(),
            amountSms = this[SmsWalletTransactionsTable.amountSms],
            cashBalanceBefore = this[SmsWalletTransactionsTable.cashBalanceBefore]
                .toPlainString(),
            cashBalanceAfter = this[SmsWalletTransactionsTable.cashBalanceAfter]
                .toPlainString(),
            smsBalanceBefore = this[SmsWalletTransactionsTable.smsBalanceBefore],
            smsBalanceAfter = this[SmsWalletTransactionsTable.smsBalanceAfter],
            description = this[SmsWalletTransactionsTable.description],
            reference = this[SmsWalletTransactionsTable.reference],
            createdAt = this[SmsWalletTransactionsTable.createdAt]
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