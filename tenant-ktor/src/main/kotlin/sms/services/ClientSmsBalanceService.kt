package sms.services

import com.example.academics.repos.setTenantSchema
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import sms.dto.SmsBalanceResponse
import sms.tables.SmsWalletsTable

object ClientSmsBalanceService {

    fun getSmsBalanceByTenantCode(
        tenantCode: String
    ): SmsBalanceResponse {

        val normalizedTenantCode =
            normalizeTenantCode(
                tenantCode
            )

        require(
            normalizedTenantCode.isNotBlank()
        ) {
            "Tenant code is required."
        }

        return transaction {

            setTenantSchema(
                "public"
            )

            val walletRow =
                SmsWalletsTable
                    .selectAll()
                    .where {
                        SmsWalletsTable.tenantCode eq normalizedTenantCode
                    }
                    .limit(1)
                    .singleOrNull()

            SmsBalanceResponse(
                tenantCode = normalizedTenantCode,
                smsBalance = walletRow
                    ?.get(SmsWalletsTable.smsBalance)
                    ?: 0
            )
        }
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