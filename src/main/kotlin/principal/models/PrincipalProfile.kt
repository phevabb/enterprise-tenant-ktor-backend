package com.example.principal.models



import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class PrincipalProfile(
    val id: Int,

    @Contextual
    val user: Int? // nullable because SET_NULL
)

