package com.example.config

import chat.tables.ChatConversationsTable
import chat.tables.ChatMessagesTable
import com.example.tenant.tables.TenantFeaturesTable
import com.example.tenant.tables.TenantsTable
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import sms.tables.SmsAnnouncementsTable
import sms.tables.SmsCampaignsTable
import sms.tables.SmsSenderIdsTable
import sms.tables.SmsWalletTransactionsTable
import sms.tables.SmsWalletsTable

object DatabaseFactory {

    private lateinit var database: Database

    fun init() {

        val hikariConfig = HikariConfig().apply {
            jdbcUrl = System.getenv("TENANT_DB_URL")
                ?: "jdbc:postgresql://localhost:5432/ktphena"

            username = System.getenv("TENANT_DB_USER")
                ?: "postgres"

            password = System.getenv("TENANT_DB_PASSWORD")
                ?: "postgres"

            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 10
            minimumIdle = 2
            isAutoCommit = false
            initializationFailTimeout = -1

            validate()
        }

        val dataSource = HikariDataSource(hikariConfig)

        database = Database.connect(dataSource)

        transaction(database) {
            SchemaUtils.create(
                TenantsTable,
                SmsSenderIdsTable,
                SmsCampaignsTable,
                TenantFeaturesTable,
                SmsWalletsTable,
                SmsWalletTransactionsTable,
                SmsAnnouncementsTable,
                ChatConversationsTable,
                ChatMessagesTable,
            )

            SchemaUtils.addMissingColumnsStatements(
                TenantsTable,
                SmsCampaignsTable,
                SmsWalletTransactionsTable,
                SmsWalletsTable,
                SmsSenderIdsTable,
                SmsAnnouncementsTable,
                TenantFeaturesTable,
                ChatConversationsTable,
                ChatMessagesTable,
            ).forEach { statement ->
                exec(statement)
            }
        }
    }

    suspend fun <T> dbQuery(
        tenantSchema: String,
        block: suspend Transaction.() -> T,
    ): T {
        require(
            tenantSchema.matches(
                Regex("^tenant_[a-zA-Z0-9_]+$"),
            ),
        ) {
            "Invalid tenant schema."
        }

        return newSuspendedTransaction(
            context = Dispatchers.IO,
            db = database,
        ) {
            exec(
                """SET LOCAL search_path TO "$tenantSchema", public""",
            )

            block()
        }
    }
}