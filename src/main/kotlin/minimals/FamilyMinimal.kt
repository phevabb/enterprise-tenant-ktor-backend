package com.example.minimals

import kotlinx.serialization.Serializable

@Serializable
data class FamilyMinimal(
    val id: Int,
    val name: String,
)
