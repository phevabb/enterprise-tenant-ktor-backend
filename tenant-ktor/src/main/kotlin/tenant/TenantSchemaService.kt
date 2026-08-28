package com.example.tenant

import chat.tables.ChatConversationsTable
import chat.tables.ChatMessagesTable
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
import complaints.tables.ComplaintRepliesTable
import complaints.tables.ParentComplaintsTable
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
                StudentBillItemsTable,

            )

            SchemaUtils.addMissingColumnsStatements(
                BillTemplatesTable,
                BillTemplateItemsTable,
                StudentBillsTable,
                StudentBillItemsTable,


            ).forEach { statement ->
                exec(statement)
            }
        }
    }


    fun createTenantSchema(
        tenantSchema: String
    ) {
        require(
            tenantSchema.isNotBlank()
        ) {
            "Tenant schema is required."
        }

        transaction {
            setTenantSchema(
                tenantSchema
            )

            println(
                "[TenantSchemaService] " +
                        "Creating tenant tables: " +
                        "tenantSchema=$tenantSchema"
            )

            SchemaUtils.create(
                AccountTable,

                AcademicYearTable,
                TermTable,
                CategoriesTable,
                NewGradeClassTable,

                FamilyTable,

                StudentsTable,
                StaffTable,
                AdminTable,
                PrincipalTable,

                FeeStructureTable,
                StudentFeeRecordTable,
                PaymentTable,
                ReceiptsTable,

                FamilyFeeRecordTable,
                FamilyPaymentTable,
                FamilyReceiptsTable,

                NewClassPromotionTable,

                SubjectsTable,
                SubjectCategoriesTable,
                SubjectCategorySubjectsTable,
                AcademicRecordsTable,
                GradesTable,
                SubjectScoresTable,

                BillTemplatesTable,
                BillTemplateItemsTable,
                StudentBillsTable,
                StudentBillItemsTable,

                ChatConversationsTable,
                ChatMessagesTable,

                ParentComplaintsTable,
                ComplaintRepliesTable
            )

            SchemaUtils.addMissingColumnsStatements(
                AccountTable,

                AcademicYearTable,
                TermTable,
                CategoriesTable,
                NewGradeClassTable,

                FamilyTable,

                StudentsTable,
                StaffTable,
                AdminTable,
                PrincipalTable,

                FeeStructureTable,
                StudentFeeRecordTable,
                PaymentTable,
                ReceiptsTable,

                FamilyFeeRecordTable,
                FamilyPaymentTable,
                FamilyReceiptsTable,

                NewClassPromotionTable,

                SubjectsTable,
                SubjectCategoriesTable,
                SubjectCategorySubjectsTable,
                AcademicRecordsTable,
                GradesTable,
                SubjectScoresTable,

                BillTemplatesTable,
                BillTemplateItemsTable,
                StudentBillsTable,
                StudentBillItemsTable,

                ChatConversationsTable,
                ChatMessagesTable,

                ParentComplaintsTable,
                ComplaintRepliesTable
            ).forEach { statement ->
                println(
                    "[TenantSchemaService] " +
                            "Executing tenant migration: " +
                            statement
                )

                exec(
                    statement
                )
            }
        }

        println(
            "[TenantSchemaService] " +
                    "Tenant tables ready: " +
                    "tenantSchema=$tenantSchema"
        )
    }

    fun ensureChatTablesForTenant(
        tenantSchema: String
    ) {
        require(
            tenantSchema.isNotBlank()
        ) {
            "Tenant schema is required."
        }

        transaction {
            setTenantSchema(
                tenantSchema
            )

            println(
                "[TenantSchemaService] " +
                        "Checking chat tables: " +
                        "tenantSchema=$tenantSchema"
            )

            SchemaUtils.create(
                ChatConversationsTable,
                ChatMessagesTable
            )

            SchemaUtils.addMissingColumnsStatements(
                ChatConversationsTable,
                ChatMessagesTable
            ).forEach { statement ->
                println(
                    "[TenantSchemaService] " +
                            "Executing chat migration: " +
                            statement
                )

                exec(
                    statement
                )
            }
        }

        println(
            "[TenantSchemaService] " +
                    "Chat tables ready: " +
                    "tenantSchema=$tenantSchema"
        )
    }


    fun ensureComplaintTablesForTenant(
        tenantSchema: String
    ) {
        require(
            tenantSchema.isNotBlank()
        ) {
            "Tenant schema is required."
        }

        transaction {
            setTenantSchema(
                tenantSchema
            )

            println(
                "[TenantSchemaService] " +
                        "Checking complaint tables: " +
                        "tenantSchema=$tenantSchema"
            )

            SchemaUtils.create(
                ParentComplaintsTable,
                ComplaintRepliesTable
            )

            SchemaUtils.addMissingColumnsStatements(
                ParentComplaintsTable,
                ComplaintRepliesTable
            ).forEach { statement ->
                println(
                    "[TenantSchemaService] " +
                            "Executing complaint migration: " +
                            statement
                )

                exec(
                    statement
                )
            }
        }

        println(
            "[TenantSchemaService] " +
                    "Complaint tables ready: " +
                    "tenantSchema=$tenantSchema"
        )
    }



}