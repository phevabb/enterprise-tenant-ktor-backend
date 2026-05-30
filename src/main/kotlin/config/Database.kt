package com.example.config

import com.example.academics.tables.AcademicRecordsTable
import com.example.academics.tables.CategoriesTable
import com.example.academics.tables.GradesTable
import com.example.academics.tables.SubjectCategoriesTable
import com.example.academics.tables.SubjectCategorySubjectsTable
import com.example.academics.tables.SubjectScoresTable
import com.example.academics.tables.SubjectsTable
import com.example. account.AccountTable
import com.example.admin.tables.AdminTable
import com.example.familyfees.tables.FamilyFeeRecordTable
import com.example.familyfees.tables.FamilyPaymentTable
import com.example.familyfees.tables.FamilyReceiptsTable
import com.example.familyfees.tables.FamilyTable
import com.example.fees.tables.FeeStructureTable
import com.example.fees.tables.PaymentTable
import com.example.fees.tables.ReceiptsTable
import com.example.fees.tables.StudentFeeRecordTable
import com.example.principal.tables.PrincipalTable
import com.example.staff.tables.StaffTable
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
            maximumPoolSize = 5
            minimumIdle = 0
            isAutoCommit = false
            initializationFailTimeout = -1
            validate()
        }
        val dataSource = HikariDataSource(hikariConfig)
        Database.connect(dataSource)   // later for neon NEON





//
//            val hikariConfig = HikariConfig().apply {
//                // ✅ Local PostgreSQL JDBC URL
//                jdbcUrl = "jdbc:postgresql://localhost:5432/ktphena"
//                username = "postgres"          // your local DB user
//                password = "postgres"     // your local DB password
//                driverClassName = "org.postgresql.Driver"
//                // ✅ Pool settings (good defaults)
//                maximumPoolSize = 10
//                minimumIdle = 2
//                isAutoCommit = false
//                // ✅ Don’t fail fast on startup
//                initializationFailTimeout = -1
//                    validate()
//            }
//            val dataSource = HikariDataSource(hikariConfig)
//            Database.connect(dataSource)  // LATER for localhost



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
                StaffTable,
                AdminTable,
                SubjectsTable,
                AcademicRecordsTable,
                GradesTable,
                SubjectScoresTable,
                CategoriesTable,
                SubjectCategorySubjectsTable,
                SubjectCategoriesTable,
                ReceiptsTable,
                FamilyReceiptsTable,
                PrincipalTable
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
                StaffTable,
                AdminTable,
                SubjectsTable,
                AcademicRecordsTable,
                GradesTable,
                SubjectScoresTable,
                CategoriesTable,
                SubjectCategorySubjectsTable,
                SubjectCategoriesTable,
                ReceiptsTable,
                FamilyReceiptsTable,
                PrincipalTable


            ).forEach { exec(it) }
        }
    }
}







