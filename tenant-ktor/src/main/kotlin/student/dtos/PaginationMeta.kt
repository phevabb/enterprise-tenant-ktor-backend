package com.example.student.dtos



@kotlinx.serialization.Serializable
data class PaginationMeta(
    val page: Int,
    val limit: Int,
    val total: Long,
    val totalPages: Int
)

@kotlinx.serialization.Serializable
data class PaginatedResponse<T>(
    val data: List<T>,
    val meta: PaginationMeta
)