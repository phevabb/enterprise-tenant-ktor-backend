package com.example.fees.dtos.requests

import kotlinx.serialization.Serializable

@Serializable
data class CreateFeeStructureRequest(

    val academic_year_id: Int,
    val grade_class_id: Int,
    val term_id: Int,
    val amount: Int,
    val is_discounted: Boolean,

)
