package com.example.config

import com.example.tenant.tables.TenantFeaturesTable
import com.example.tenant.tables.TenantsTable
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import jdk.internal.org.jline.utils.ExecHelper.exec
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {

    fun init() {

        val hikariConfig = HikariConfig().apply {
            jdbcUrl = "jdbc:postgresql://localhost:5432/ktphena"
            username = "postgres"
            password = "postgres"
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
                TenantFeaturesTable
            )

            SchemaUtils.addMissingColumnsStatements(
                TenantsTable,
                TenantFeaturesTable
            ).forEach { exec(it) }
        }

    }
}