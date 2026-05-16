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
import com.example.student.tables.NewClassPromotionTable
import com.example.student.tables.NewGradeClassTable
import com.example.student.tables.TermTable
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

//
//object DatabaseFactory {
//
//    fun init() {
//
//        val hikariConfig = HikariConfig().apply {
//
//            // ✅ Neon PostgreSQL JDBC URL
//            jdbcUrl =
//                "jdbc:postgresql://ep-proud-sound-andkoqj6-pooler.c-6.us-east-1.aws.neon.tech/neondb"
//
//            username = "neondb_owner"
//            password = "npg_ETm9p5IrkyjY"
//
//            driverClassName = "org.postgresql.Driver"
//
//            // ✅ REQUIRED for Neon
//            addDataSourceProperty("sslmode", "require")
//            addDataSourceProperty("channelBinding", "require")
//
//            maximumPoolSize = 5
//            isAutoCommit = false
//            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
//
//            validate()
//        }
//
//        val dataSource = HikariDataSource(hikariConfig)
//        Database.connect(dataSource)
//
//        transaction {
//            SchemaUtils.create(
//                AccountTable,
//                NewGradeClassTable,
//                StudentsTable,
//                AcademicYearTable,
//                TermTable,
//                FeeStructureTable,
//                StudentFeeRecordTable,
//                PaymentTable,
//                FamilyTable,
//                FamilyFeeRecordTable,
//                FamilyPaymentTable
//            )
//
//            SchemaUtils.addMissingColumnsStatements(
//                AccountTable,
//                NewGradeClassTable,
//                StudentsTable,
//                AcademicYearTable,
//                TermTable,
//                FamilyPaymentTable,
//                FeeStructureTable,
//                StudentFeeRecordTable,
//                PaymentTable,
//                FamilyTable,
//                FamilyFeeRecordTable
//            ).forEach { exec(it) }
//        }
//    }
//}
//


object DatabaseFactory {

    fun init() {



        val hikariConfig = HikariConfig().apply {

            jdbcUrl =
                "jdbc:postgresql://ep-proud-sound-andkoqj6-pooler.c-6.us-east-1.aws.neon.tech/neondb"

            username = "neondb_owner"
            password = "npg_ETm9p5IrkyjY"

            driverClassName = "org.postgresql.Driver"

            // Neon requires SSL
            addDataSourceProperty("sslmode", "require")

            // Channel binding is optional — remove if you still see connection issues
            // addDataSourceProperty("channelBinding", "require")

            maximumPoolSize = 5
            minimumIdle = 0
            isAutoCommit = false

            // ✅ Important: DO NOT set transactionIsolation for Neon pooler
            // transactionIsolation = "TRANSACTION_REPEATABLE_READ"

            // Optional: avoid failing immediately if DB is warming up
            initializationFailTimeout = -1

            validate()
        }

        val dataSource = HikariDataSource(hikariConfig)
        Database.connect(dataSource)   // later for neon


//        val hikariConfig = HikariConfig().apply {
//
//            // ✅ Supabase PostgreSQL JDBC URL
//            jdbcUrl = "jdbc:postgresql://db.ezawszqcemmvmpcxtljm.supabase.co:5432/postgres"
//
//            username = "postgres"
//            password = "Uncleproton1.postgres"
//
//            driverClassName = "org.postgresql.Driver"
//
//            // ✅ Supabase requires SSL
//            addDataSourceProperty("sslmode", "require")
//
//            maximumPoolSize = 5
//            minimumIdle = 0
//            isAutoCommit = false
//
//            // ✅ Keep this: avoids failing immediately if DB is warming up
//            initializationFailTimeout = -1
//
//            // ❌ Do not set transaction isolation here (especially if you later use poolers)
//            // transactionIsolation = "TRANSACTION_REPEATABLE_READ"
//
//            validate()
//        }
//
//        val dataSource = HikariDataSource(hikariConfig)
//        Database.connect(dataSource)   //  super base



//            val hikariConfig = HikariConfig().apply {
//
//                // ✅ Local PostgreSQL JDBC URL
//                jdbcUrl = "jdbc:postgresql://localhost:5432/ktphena"
//
//                username = "postgres"          // your local DB user
//                password = "postgres"     // your local DB password
//
//                driverClassName = "org.postgresql.Driver"
//
//                // ✅ Pool settings (good defaults)
//                maximumPoolSize = 10
//                minimumIdle = 2
//                isAutoCommit = false
//
//                // ✅ Don’t fail fast on startup
//                initializationFailTimeout = -1
//
//                // ❌ NO SSL for local
//                // ❌ NO transactionIsolation override
//
//                validate()
//            }
//
//            val dataSource = HikariDataSource(hikariConfig)
//
//            Database.connect(dataSource)  // LATER for local



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
                FamilyPaymentTable,
                NewClassPromotionTable,
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
                FamilyFeeRecordTable,
                NewClassPromotionTable,


            ).forEach { exec(it) }
        }
    }
}







