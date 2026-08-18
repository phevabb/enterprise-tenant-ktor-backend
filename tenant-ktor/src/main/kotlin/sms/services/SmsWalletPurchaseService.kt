package sms.services

import com.example.academics.repos.setTenantSchema
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import sms.dto.PurchaseSmsCreditsRequest
import sms.dto.PurchaseSmsCreditsResponse
import sms.tables.SmsWalletTransactionsTable
import sms.tables.SmsWalletsTable
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

object SmsWalletPurchaseService {

    /*
     * This is the selling price per SMS unit.
     * GHS 0.0550 means:
     * GHS 10.00 = floor(10 / 0.0550) = 181 SMS units
     */
    private val smsSellingPrice =
        BigDecimal("0.0550")

    fun purchaseSmsCredits(
        request: PurchaseSmsCreditsRequest
    ): PurchaseSmsCreditsResponse {

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

        require(
            normalizedTenantCode.isNotBlank()
        ) {
            "Tenant code is required."
        }

        require(
            amount >= BigDecimal("10.00")
        ) {
            "You cannot buy less than GHS 10.00 worth of SMS."
        }

        require(
            amount <= BigDecimal("100.00")
        ) {
            "You cannot buy more than GHS 100.00 worth of SMS at once."
        }

        require(
            isWholeCediAmount(
                amount
            )
        ) {
            "Decimals are not allowed. Please enter a whole cedi amount."
        }

        val calculatedSmsCredits =
            amount
                .divideToIntegralValue(
                    smsSellingPrice
                )
                .toInt()

        require(
            calculatedSmsCredits > 0
        ) {
            "SMS credits could not be calculated."
        }

        /*
         * Important security check:
         * The frontend may send smsCredits, but the backend must verify it.
         */
        require(
            request.smsCredits == calculatedSmsCredits
        ) {
            "SMS credits mismatch. Expected $calculatedSmsCredits SMS credits for GHS ${amount.toPlainString()}."
        }

        val now =
            Instant.now().toString()

        val reference =
            "sms_purchase_${normalizedTenantCode}_${System.currentTimeMillis()}"

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
                    ?: throw IllegalArgumentException(
                        "Wallet not found. Please load wallet first."
                    )

            val walletId =
                walletRow[SmsWalletsTable.id]

            val oldCashBalance =
                walletRow[SmsWalletsTable.cashBalance]

            val oldSmsBalance =
                walletRow[SmsWalletsTable.smsBalance]

            val oldTotalSmsPurchased =
                walletRow[SmsWalletsTable.totalSmsPurchased]

            require(
                oldCashBalance >= amount
            ) {
                "Insufficient wallet balance. Please load your wallet."
            }

            val newCashBalance =
                oldCashBalance - amount

            val newSmsBalance =
                oldSmsBalance + calculatedSmsCredits

            val newTotalSmsPurchased =
                oldTotalSmsPurchased + calculatedSmsCredits

            SmsWalletsTable.update(
                {
                    SmsWalletsTable.id eq walletId
                }
            ) {

                it[cashBalance] =
                    newCashBalance

                it[smsBalance] =
                    newSmsBalance

                it[totalSmsPurchased] =
                    newTotalSmsPurchased

                it[updatedAt] =
                    now
            }

            SmsWalletTransactionsTable.insert {

                it[tenantCode] =
                    normalizedTenantCode

                /*
                 * This is an SMS credit transaction because SMS balance increases.
                 */
                it[type] =
                    "sms_credit"

                /*
                 * Cash amount deducted from wallet.
                 */
                it[amountCash] =
                    amount

                /*
                 * SMS units added to wallet.
                 */
                it[amountSms] =
                    calculatedSmsCredits

                it[cashBalanceBefore] =
                    oldCashBalance

                it[cashBalanceAfter] =
                    newCashBalance

                it[smsBalanceBefore] =
                    oldSmsBalance

                it[smsBalanceAfter] =
                    newSmsBalance

                it[description] =
                    "Purchased SMS credits from wallet"

                it[SmsWalletTransactionsTable.reference] =
                    reference

                it[createdAt] =
                    now
            }

            PurchaseSmsCreditsResponse(
                success = true,
                message = "SMS credits purchased successfully.",
                tenantCode = normalizedTenantCode,
                amountSpent = amount.toPlainString(),
                smsCreditsPurchased = calculatedSmsCredits,
                cashBalance = newCashBalance.toPlainString(),
                smsBalance = newSmsBalance,
                totalSmsPurchased = newTotalSmsPurchased
            )
        }
    }

    private fun isWholeCediAmount(
        amount: BigDecimal
    ): Boolean {

        return amount
            .stripTrailingZeros()
            .scale() <= 0
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