package com.example.fees.dtos.requests

import kotlinx.serialization.Serializable

@Serializable
data class PatchFeeStructureRequest(
    val academic_year_id: Int? = null,
    val grade_class_id: Int? = null,
    val term_id: Int? = null,
    val amount: Int? = null,
    val is_discounted: Boolean? = null,

)
