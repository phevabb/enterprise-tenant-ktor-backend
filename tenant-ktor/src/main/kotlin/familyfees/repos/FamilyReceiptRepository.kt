package com.example.familyfees.repos

import com.example.familyfees.dtos.responses.FamilyReceiptDto
import com.example.familyfees.tables.FamilyReceiptsTable
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Transaction
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.random.Random

object FamilyReceiptRepository {

    private fun generateReceiptNo(): String {
        val date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
        val rand = Random.nextInt(100000, 999999)
        return "FAM-$date-$rand"
    }

    private fun Transaction.setTenantSchema(tenantSchema: String) {
        setTenantSchema(tenantSchema)
    }

    fun createReceipt(
        tenantSchema: String,
        familyPaymentId: Int,
        familyFeeRecordId: Int,
        familyName: String,
        wards: List<String>,
        amountPaid: Int,
        balanceAfter: Int,
        paymentMethod: String,
        termName: String?,
        academicYearName: String?
    ): FamilyReceiptDto = transaction {

        setTenantSchema(tenantSchema)

        val receiptNo = generateReceiptNo()
        val wardsCsv = wards.joinToString(", ")

        val id = FamilyReceiptsTable.insertAndGetId {
            it[FamilyReceiptsTable.receiptNo] = receiptNo
            it[FamilyReceiptsTable.familyPaymentId] = familyPaymentId
            it[FamilyReceiptsTable.familyFeeRecordId] = familyFeeRecordId
            it[FamilyReceiptsTable.familyName] = familyName
            it[FamilyReceiptsTable.wardsCsv] = wardsCsv
            it[FamilyReceiptsTable.amountPaid] = amountPaid
            it[FamilyReceiptsTable.balanceAfter] = balanceAfter
            it[FamilyReceiptsTable.paymentMethod] = paymentMethod
            it[FamilyReceiptsTable.termName] = termName
            it[FamilyReceiptsTable.academicYearName] = academicYearName
            it[FamilyReceiptsTable.createdAt] = System.currentTimeMillis()
        }.value

        println(
            "✅ FAMILY RECEIPT CREATED: " +
                    "tenant=$tenantSchema " +
                    "id=$id " +
                    "no=$receiptNo " +
                    "pdfUrl=/api/family-receipts/$id/pdf"
        )

        FamilyReceiptDto(
            id = id,
            receiptNo = receiptNo,
            familyPaymentId = familyPaymentId,
            familyFeeRecordId = familyFeeRecordId,
            familyName = familyName,
            wards = wards,
            amountPaid = amountPaid,
            balanceAfter = balanceAfter,
            paymentMethod = paymentMethod,
            termName = termName,
            academicYearName = academicYearName,
            createdAt = System.currentTimeMillis(),
            pdfUrl = "/api/family-receipts/$id/pdf"
        )
    }

    fun findById(
        tenantSchema: String,
        id: Int
    ): FamilyReceiptDto? = transaction {

        setTenantSchema(tenantSchema)

        FamilyReceiptsTable
            .selectAll()
            .where { FamilyReceiptsTable.id eq id }
            .singleOrNull()
            ?.let { row ->

                val wards = row[FamilyReceiptsTable.wardsCsv]
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }

                FamilyReceiptDto(
                    id = row[FamilyReceiptsTable.id].value,
                    receiptNo = row[FamilyReceiptsTable.receiptNo],
                    familyPaymentId = row[FamilyReceiptsTable.familyPaymentId],
                    familyFeeRecordId = row[FamilyReceiptsTable.familyFeeRecordId],
                    familyName = row[FamilyReceiptsTable.familyName],
                    wards = wards,
                    amountPaid = row[FamilyReceiptsTable.amountPaid],
                    balanceAfter = row[FamilyReceiptsTable.balanceAfter],
                    paymentMethod = row[FamilyReceiptsTable.paymentMethod],
                    termName = row[FamilyReceiptsTable.termName],
                    academicYearName = row[FamilyReceiptsTable.academicYearName],
                    createdAt = row[FamilyReceiptsTable.createdAt],
                    pdfUrl = "/api/family-receipts/${row[FamilyReceiptsTable.id].value}/pdf"
                )
            }
    }
}