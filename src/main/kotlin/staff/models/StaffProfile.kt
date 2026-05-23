package com.example.staff.models


import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class StaffProfile(
    val id: Int,

    @Contextual
    val user: Int?,   // nullable because SET_NULL

    val assignedClassId: Int?,  // nullable FK

    val tel: String?
)