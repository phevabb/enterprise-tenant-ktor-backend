package com.example.fees.dtos.responses

import com.example.minimals.StudentFeeRecordMinimal
import kotlinx.serialization.Serializable

@Serializable
data class PaymentResponseDto(
    val id: Int,
    val student_fee_record: StudentFeeRecordMinimal,
    val amount: Int,
    val date_created: Long,
    val balance: Int,
    val receipt: ReceiptResponse?=null


    )
