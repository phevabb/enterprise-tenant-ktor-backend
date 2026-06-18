package com.example.minimals

import kotlinx.serialization.Serializable

@Serializable
data class StudentFeeRecordMinimal(

    val id : Int,
    val student : StudentNameOnly,
    val feeStructure: FeeStructureMinimal

)
