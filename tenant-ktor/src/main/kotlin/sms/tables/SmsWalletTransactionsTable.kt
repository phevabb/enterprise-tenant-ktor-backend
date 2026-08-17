package sms.tables




import org.jetbrains.exposed.dao.id.IntIdTable
import java.math.BigDecimal

object SmsWalletTransactionsTable : IntIdTable("sms_wallet_transactions") {

    val tenantCode =
        varchar(
            name = "tenant_code",
            length = 100
        )

    val type =
        varchar(
            name = "type",
            length = 40
        )

    val amountCash =
        decimal(
            name = "amount_cash",
            precision = 12,
            scale = 2
        ).nullable()

    val amountSms =
        integer(
            name = "amount_sms"
        ).nullable()

    val cashBalanceBefore =
        decimal(
            name = "cash_balance_before",
            precision = 12,
            scale = 2
        )

    val cashBalanceAfter =
        decimal(
            name = "cash_balance_after",
            precision = 12,
            scale = 2
        )

    val smsBalanceBefore =
        integer(
            name = "sms_balance_before"
        )

    val smsBalanceAfter =
        integer(
            name = "sms_balance_after"
        )

    val description =
        varchar(
            name = "description",
            length = 500
        )

    val reference =
        varchar(
            name = "reference",
            length = 300
        ).nullable()

    val createdAt =
        varchar(
            name = "created_at",
            length = 50
        )
}


