package com.example.config

import com.example.account.AccountTable
import com.example.fees.tables.FeeStructureTable
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
            jdbcUrl = "jdbc:postgresql://localhost:5432/ktphena"
            driverClassName = "org.postgresql.Driver"
            username = "postgres"
            password = "postgres"
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }

        val ds = HikariDataSource(hikariConfig)
        Database.connect(ds)

        transaction {
            // 1) Create tables if missing
            SchemaUtils.create(AccountTable, NewGradeClassTable, StudentsTable, AcademicYearTable,
                TermTable, FeeStructureTable, StudentFeeRecordTable
            )

            // 2) Add missing columns (DEV convenience)
            val statements = SchemaUtils.addMissingColumnsStatements(
                AccountTable, NewGradeClassTable, StudentsTable,
                AcademicYearTable,TermTable, FeeStructureTable, StudentFeeRecordTable
            )



            statements.forEach { stmt -> exec(stmt) }
        }
    }
}