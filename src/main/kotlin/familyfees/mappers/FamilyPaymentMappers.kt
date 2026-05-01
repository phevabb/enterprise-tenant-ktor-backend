package com.example.familyfees.mappers

import com.example.familyfees.models.FamilyPaymentModel
import com.example.familyfees.tables.FamilyPaymentTable
import com.example.familyfees.tables.FamilyPaymentTable.family_fee_record
import com.example.fees.tables.PaymentTable
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toFamilyPaymentModel() = FamilyPaymentModel(
    id = this[FamilyPaymentTable.id].value,
    family_fee_record = this[FamilyPaymentTable.family_fee_record].value,
    payment_method = this[FamilyPaymentTable.payment_method],
    amount = this[FamilyPaymentTable.amount],
    date_created = this[FamilyPaymentTable.date_created],
)