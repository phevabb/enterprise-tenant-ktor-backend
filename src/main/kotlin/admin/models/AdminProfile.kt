package com.example.admin.models





import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class AdminProfile(
    val id: Int,

    @Contextual
    val user: Int?,   // nullable because SET_NULL

    val tel: String?
)