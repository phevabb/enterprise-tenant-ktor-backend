package com.example.config

import chat.tables.ChatConversationsTable
import chat.tables.ChatMessagesTable
import com.example.tenant.tables.TenantFeaturesTable
import com.example.tenant.tables.TenantsTable
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import sms.tables.SmsAnnouncementsTable
import sms.tables.SmsCampaignsTable
import sms.tables.SmsSenderIdsTable
import sms.tables.SmsWalletTransactionsTable
import sms.tables.SmsWalletsTable

object DatabaseFactory {

    fun init() {

        val hikariConfig = HikariConfig().apply {
            jdbcUrl = System.getenv("TENANT_DB_URL")
                ?: "jdbc:postgresql://localhost:5432/ktphena"

            username = System.getenv("TENANT_DB_USER") ?: "postgres"
            password = System.getenv("TENANT_DB_PASSWORD") ?: "postgres"

            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 10
            minimumIdle = 2
            isAutoCommit = false
            initializationFailTimeout = -1
            validate()
        }

        val dataSource = HikariDataSource(hikariConfig)
        Database.connect(dataSource)

        transaction {
            SchemaUtils.create(
                TenantsTable,
                SmsSenderIdsTable,
                SmsCampaignsTable,
                TenantFeaturesTable,
                SmsWalletsTable,
                SmsWalletTransactionsTable,
                SmsAnnouncementsTable,





            )

            SchemaUtils.addMissingColumnsStatements(
                TenantsTable,
                SmsCampaignsTable,
                SmsWalletTransactionsTable,
                SmsWalletsTable,
                SmsSenderIdsTable,
                SmsAnnouncementsTable,

                TenantFeaturesTable
            ).forEach { exec(it) }
        }
    }
}