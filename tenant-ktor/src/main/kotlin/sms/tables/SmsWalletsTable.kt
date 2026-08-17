package sms.tables



import java.math.BigDecimal
import org.jetbrains.exposed.dao.id.IntIdTable


object SmsWalletsTable : IntIdTable("sms_wallets") {

    val tenantCode =
        varchar(
            name = "tenant_code",
            length = 100
        ).uniqueIndex()

    val schoolName =
        varchar(
            name = "school_name",
            length = 255
        )

    val cashBalance =
        decimal(
            name = "cash_balance",
            precision = 12,
            scale = 2
        ).default(BigDecimal("0.00"))

    val smsBalance =
        integer(
            name = "sms_balance"
        ).default(0)

    val totalCashLoaded =
        decimal(
            name = "total_cash_loaded",
            precision = 12,
            scale = 2
        ).default(BigDecimal("0.00"))

    val totalSmsPurchased =
        integer(
            name = "total_sms_purchased"
        ).default(0)

    val totalSmsUsed =
        integer(
            name = "total_sms_used"
        ).default(0)

    val status =
        varchar(
            name = "status",
            length = 30
        ).default("active")

    val createdAt =
        varchar(
            name = "created_at",
            length = 50
        )

    val updatedAt =
        varchar(
            name = "updated_at",
            length = 50
        ).nullable()
}