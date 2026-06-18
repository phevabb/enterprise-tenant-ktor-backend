package com.example.familyfees.dtos.requests

import kotlinx.serialization.Serializable


@Serializable
data class CreateFamilyRequest(
    val name: String,

)
