package com.example.fees.models

import com.example.minimals.FeeStructureMinimal
import com.example.minimals.StudentNameOnly
import kotlinx.serialization.Serializable

@Serializable
data class StudentFeeRecordModel(
    val id: Int,
    val student: StudentNameOnly,
    val feeStructure: FeeStructureMinimal,
    val amountPaid: Int,
    val balance: Int,
    val isFullyPaid: Boolean,
    val dateCreated: Long
)
