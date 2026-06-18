package com.example.tenant.services


import com.example.academics.tables.AcademicRecordsTable
import com.example.academics.tables.CategoriesTable
import com.example.academics.tables.GradesTable
import com.example.academics.tables.SubjectCategoriesTable
import com.example.academics.tables.SubjectCategorySubjectsTable
import com.example.academics.tables.SubjectScoresTable
import com.example.academics.tables.SubjectsTable
import com.example.account.AccountTable
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
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object TenantSchemaService {

    fun createTenantSchema(tenantSchema: String) {
        transaction {
            exec("""CREATE SCHEMA IF NOT EXISTS "$tenantSchema";""")
            exec("""SET LOCAL search_path TO "$tenantSchema", public;""")

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
        }
    }
}