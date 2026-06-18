package com.example.fees.mappers

import com.example.fees.models.PaymentModel
import com.example.fees.tables.PaymentTable
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toPaymentModel() = PaymentModel(
    id = this[PaymentTable.id].value,
    student_fee_record = this[PaymentTable.student_fee_record].value,
    payment_method = this[PaymentTable.payment_method],
    amount = this[PaymentTable.amount],
    date_created = this[PaymentTable.date_created],
)

