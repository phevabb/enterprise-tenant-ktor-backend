package com.example.tenant

import com.example.academics.repos.setTenantSchema
import com.example.academics.tables.AcademicRecordsTable
import com.example.academics.tables.CategoriesTable
import com.example.academics.tables.GradesTable
import com.example.academics.tables.SubjectCategoriesTable
import com.example.academics.tables.SubjectCategorySubjectsTable
import com.example.academics.tables.SubjectScoresTable
import com.example.academics.tables.SubjectsTable
import com.example.account.AccountTable
import com.example.admin.tables.AdminTable
import com.example.billing.tables.BillTemplateItemsTable
import com.example.billing.tables.BillTemplatesTable
import com.example.billing.tables.StudentBillItemsTable
import com.example.billing.tables.StudentBillsTable
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

    fun ensureBillingTablesForTenant(tenantSchema: String) {
        transaction {
            setTenantSchema(tenantSchema)

            SchemaUtils.create(
                BillTemplatesTable,
                BillTemplateItemsTable,
                StudentBillsTable,
                StudentBillItemsTable
            )

            SchemaUtils.addMissingColumnsStatements(
                BillTemplatesTable,
                BillTemplateItemsTable,
                StudentBillsTable,
                StudentBillItemsTable
            ).forEach { statement ->
                exec(statement)
            }
        }
    }


    fun createTenantSchema(tenantSchema: String) {
        transaction {
            setTenantSchema(tenantSchema)

            SchemaUtils.create(
                AccountTable,
                NewGradeClassTable,
                StudentsTable,
                AcademicYearTable,
                TermTable,

                FeeStructureTable,
                StudentFeeRecordTable,
                PaymentTable,
                ReceiptsTable,

                FamilyTable,
                FamilyFeeRecordTable,
                FamilyPaymentTable,
                FamilyReceiptsTable,

                NewClassPromotionTable,
                StaffTable,
                AdminTable,
                PrincipalTable,

                SubjectsTable,
                AcademicRecordsTable,
                GradesTable,
                SubjectScoresTable,
                CategoriesTable,
                SubjectCategorySubjectsTable,
                SubjectCategoriesTable,

                BillTemplatesTable,
                BillTemplateItemsTable,
                StudentBillsTable,
                StudentBillItemsTable
            )

            SchemaUtils.addMissingColumnsStatements(
                AccountTable,
                NewGradeClassTable,
                StudentsTable,
                AcademicYearTable,
                TermTable,

                FeeStructureTable,
                StudentFeeRecordTable,
                PaymentTable,
                ReceiptsTable,

                FamilyTable,
                FamilyFeeRecordTable,
                FamilyPaymentTable,
                FamilyReceiptsTable,

                NewClassPromotionTable,
                StaffTable,
                AdminTable,
                PrincipalTable,

                SubjectsTable,
                AcademicRecordsTable,
                GradesTable,
                SubjectScoresTable,
                CategoriesTable,
                SubjectCategorySubjectsTable,
                SubjectCategoriesTable,

                BillTemplatesTable,
                BillTemplateItemsTable,
                StudentBillsTable,
                StudentBillItemsTable
            ).forEach { statement ->
                exec(statement)
            }
        }
    }
}