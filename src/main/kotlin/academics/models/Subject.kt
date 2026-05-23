package com.example.academics.models


import kotlinx.serialization.Serializable

@Serializable
data class Subject(
    val id: Int,
    val name: String
)