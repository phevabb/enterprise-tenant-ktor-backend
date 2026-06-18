package com.example.familyfees.dtos.responses

import com.example.minimals.FamilyFeeRecordMinimal
import com.example.minimals.StudentFeeRecordMinimal
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class FamilyPaymentResponseDto(

    val id: Int,
    @Contextual
    val family_fee_record: FamilyFeeRecordMinimal,
    val amount: Int,
    val date_created: Long,
    val balance: Int,
    val wards: List<String> = emptyList(),
    val receipt: FamilyReceiptDto? = null
)
