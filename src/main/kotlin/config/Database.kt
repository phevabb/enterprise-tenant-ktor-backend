package com.example.config

import com.example. account.AccountTable
import com.example.familyfees.tables.FamilyFeeRecordTable
import com.example.familyfees.tables.FamilyPaymentTable
import com.example.familyfees.tables.FamilyTable
import com.example.fees.tables.FeeStructureTable
import com.example.fees.tables.PaymentTable
import com.example.fees.tables.StudentFeeRecordTable

import com.example.student.StudentsTable
import com.example.student.tables.AcademicYearTable
import com.example.student.tables.NewGradeClassTable
import com.example.student.tables.TermTable
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction


object DatabaseFactory {

    fun init() {

        val hikariConfig = HikariConfig().apply {

            // ✅ Neon PostgreSQL JDBC URL
            jdbcUrl =
                "jdbc:postgresql://ep-proud-sound-andkoqj6-pooler.c-6.us-east-1.aws.neon.tech/neondb"

            username = "neondb_owner"
            password = "npg_ETm9p5IrkyjY"

            driverClassName = "org.postgresql.Driver"

            // ✅ REQUIRED for Neon
            addDataSourceProperty("sslmode", "require")
            addDataSourceProperty("channelBinding", "require")

            maximumPoolSize = 5
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"

            validate()
        }

        val dataSource = HikariDataSource(hikariConfig)
        Database.connect(dataSource)

        transaction {
            SchemaUtils.create(
                AccountTable,
                NewGradeClassTable,
                StudentsTable,
                AcademicYearTable,
                TermTable,
                FeeStructureTable,
                StudentFeeRecordTable,
                PaymentTable,
                FamilyTable,
                FamilyFeeRecordTable,
                FamilyPaymentTable
            )

            SchemaUtils.addMissingColumnsStatements(
                AccountTable,
                NewGradeClassTable,
                StudentsTable,
                AcademicYearTable,
                TermTable,
                FamilyPaymentTable,
                FeeStructureTable,
                StudentFeeRecordTable,
                PaymentTable,
                FamilyTable,
                FamilyFeeRecordTable
            ).forEach { exec(it) }
        }
    }
}