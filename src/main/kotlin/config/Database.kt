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

            jdbcUrl =
                "jdbc:postgresql://dpg-d7q5hmcm0tmc73cs6h30-a:5432/kog_ktor_database"

            username = "kog_ktor_database_user"
            password = "tcGOUiie0HB1tSuZ2Y7UYdtxxnriLeMn"

            driverClassName = "org.postgresql.Driver"

            // Render Postgres requires SSL
            addDataSourceProperty("sslmode", "require")

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

