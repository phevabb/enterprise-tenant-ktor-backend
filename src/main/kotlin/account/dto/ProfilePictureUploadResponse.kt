package com.example.account.dto



import kotlinx.serialization.Serializable

@Serializable
data class ProfilePictureUploadResponse(
    val id: Int,
    val profilePictureUrl: String,
    val profilePicturePublicId: String,
    val originalFileName: String? = null,
    val contentType: String? = null,
    val sizeBytes: Int
)
