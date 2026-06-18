package com.example.notifications

import kotlinx.serialization.Serializable

@Serializable
data class SmsPayload(
    val phone: String,
    val message: String
)



object SmsTemplates {

    fun paymentReceived(
        studentName: String,
        amountPaid: Int,
        balance: Int,
        className: String,
        term: String,
        academicYear: String
    ): String {

        val status = if (balance <= 0) "full payment" else "part payment"

        return """
            Dear Parent/Guardian,
            You made a $status of GH₵ $amountPaid for your ward, $studentName.
            Purpose: School fees
            Class/Term: $className – $term.
            Year: $academicYear academic year.
            Balance: GH₵ $balance.
            Thank you.
        """.trimIndent()
    }
}