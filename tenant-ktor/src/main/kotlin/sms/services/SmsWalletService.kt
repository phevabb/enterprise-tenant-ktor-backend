package sms.services



import com.example.academics.repos.setTenantSchema
import com.example.tenant.tables.TenantsTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import sms.dto.CreditCashWalletRequest
import sms.dto.CreditCashWalletResponse
import sms.tables.SmsWalletTransactionsTable
import sms.tables.SmsWalletsTable
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

object SmsWalletService {

    fun creditCashWallet(
        request: CreditCashWalletRequest
    ): CreditCashWalletResponse {

        val normalizedTenantCode =
            normalizeTenantCode(
                request.tenantCode
            )

        val amount =
            request.amount
                .toBigDecimalOrNull()
                ?.setScale(
                    2,
                    RoundingMode.HALF_UP
                )
                ?: throw IllegalArgumentException(
                    "Invalid amount."
                )

        val reference =
            request.reference.trim()

        require(
            normalizedTenantCode.isNotBlank()
        ) {
            "Tenant code is required."
        }

        require(
            amount > BigDecimal.ZERO
        ) {
            "Amount must be greater than zero."
        }

        require(
            reference.isNotBlank()
        ) {
            "Payment reference is required."
        }

        val now =
            Instant.now().toString()

        return transaction {

            setTenantSchema(
                "public"
            )

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

            val existingWallet =
                SmsWalletsTable
                    .selectAll()
                    .where {
                        SmsWalletsTable.tenantCode eq normalizedTenantCode
                    }
                    .limit(1)
                    .singleOrNull()

            if (existingWallet == null) {

                SmsWalletsTable.insert {

                    it[tenantCode] =
                        normalizedTenantCode

                    it[SmsWalletsTable.schoolName] =
                        schoolName

                    it[cashBalance] =
                        amount

                    it[smsBalance] =
                        0

                    it[totalCashLoaded] =
                        amount

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
                }

                SmsWalletTransactionsTable.insert {

                    it[tenantCode] =
                        normalizedTenantCode

                    it[type] =
                        "cash_credit"

                    it[amountCash] =
                        amount

                    it[amountSms] =
                        null

                    it[cashBalanceBefore] =
                        BigDecimal("0.00")

                    it[cashBalanceAfter] =
                        amount

                    it[smsBalanceBefore] =
                        0

                    it[smsBalanceAfter] =
                        0

                    it[description] =
                        "Wallet top-up via Paystack"

                    it[SmsWalletTransactionsTable.reference] =
                        reference

                    it[createdAt] =
                        now
                }

                return@transaction CreditCashWalletResponse(
                    success = true,
                    message = "Cash wallet credited successfully.",
                    tenantCode = normalizedTenantCode,
                    cashBalance = amount.toPlainString(),
                    smsBalance = 0
                )
            }

            val walletId =
                existingWallet[SmsWalletsTable.id].value

            val oldCashBalance =
                existingWallet[SmsWalletsTable.cashBalance]

            val oldSmsBalance =
                existingWallet[SmsWalletsTable.smsBalance]

            val oldTotalCashLoaded =
                existingWallet[SmsWalletsTable.totalCashLoaded]

            val newCashBalance =
                oldCashBalance + amount

            val newTotalCashLoaded =
                oldTotalCashLoaded + amount

            SmsWalletsTable.update(
                {
                    SmsWalletsTable.id eq walletId
                }
            ) {

                it[cashBalance] =
                    newCashBalance

                it[totalCashLoaded] =
                    newTotalCashLoaded

                it[updatedAt] =
                    now
            }

            SmsWalletTransactionsTable.insert {

                it[tenantCode] =
                    normalizedTenantCode

                it[type] =
                    "cash_credit"

                it[amountCash] =
                    amount

                it[amountSms] =
                    null

                it[cashBalanceBefore] =
                    oldCashBalance

                it[cashBalanceAfter] =
                    newCashBalance

                it[smsBalanceBefore] =
                    oldSmsBalance

                it[smsBalanceAfter] =
                    oldSmsBalance

                it[description] =
                    "Wallet top-up via Paystack"

                it[SmsWalletTransactionsTable.reference] =
                    reference

                it[createdAt] =
                    now
            }

            CreditCashWalletResponse(
                success = true,
                message = "Cash wallet credited successfully.",
                tenantCode = normalizedTenantCode,
                cashBalance = newCashBalance.toPlainString(),
                smsBalance = oldSmsBalance
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