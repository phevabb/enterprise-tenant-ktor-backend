package com.example.superadmin.repo




import com.example.superadmin.dto.response.SuperAdminTransactionResponse
import org.jetbrains.exposed.sql.transactions.transaction

object SuperAdminTransactionRepository {

    private const val TRANSACTIONS_TABLE = "billing_invoices"

    fun findAll(
        tenantCode: String?,
        status: String?,
        paid: Boolean?
    ): List<SuperAdminTransactionResponse> = transaction {
        val sql = """
            SELECT
                id,
                account_id,
                tenant_code,
                academic_year_id,
                academic_term_id,
                student_count,
                amount_per_student_cedis,
                total_amount_cedis,
                is_paid,
                payment_status,
                paystack_reference,
                due_date_epoch_millis,
                paid_at_epoch_millis,
                created_at_epoch_millis
            FROM $TRANSACTIONS_TABLE
            ORDER BY created_at_epoch_millis DESC
        """.trimIndent()

        val rows = mutableListOf<SuperAdminTransactionResponse>()

        exec(sql) { rs ->
            while (rs.next()) {
                rows += SuperAdminTransactionResponse(
                    id = rs.getInt("id"),
                    accountId = rs.getInt("account_id"),
                    tenantCode = rs.getString("tenant_code") ?: "",
                    academicYearId = rs.getObject("academic_year_id") as? Int,
                    academicTermId = rs.getObject("academic_term_id") as? Int,
                    studentCount = rs.getInt("student_count"),
                    amountPerStudentCedis = rs.getBigDecimal("amount_per_student_cedis")?.toDouble() ?: 0.0,
                    totalAmountCedis = rs.getBigDecimal("total_amount_cedis")?.toDouble() ?: 0.0,
                    isPaid = rs.getBoolean("is_paid"),
                    paymentStatus = rs.getString("payment_status") ?: "unknown",
                    paystackReference = rs.getString("paystack_reference"),
                    dueDateEpochMillis = rs.getObject("due_date_epoch_millis") as? Long,
                    paidAtEpochMillis = rs.getObject("paid_at_epoch_millis") as? Long,
                    createdAtEpochMillis = rs.getObject("created_at_epoch_millis") as? Long
                )
            }
        }

        rows
            .filter { item ->
                tenantCode.isNullOrBlank() ||
                        item.tenantCode.equals(tenantCode.trim(), ignoreCase = true)
            }
            .filter { item ->
                status.isNullOrBlank() ||
                        item.paymentStatus.equals(status.trim(), ignoreCase = true)
            }
            .filter { item ->
                paid == null || item.isPaid == paid
            }
    }
}