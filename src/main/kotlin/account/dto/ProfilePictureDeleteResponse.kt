package com.example.account.dto



import kotlinx.serialization.Serializable

@Serializable
data class ProfilePictureDeleteResponse(
    val id: Int,
    val deleted: Boolean,
    val deletedFromCloudinary: Boolean,
    val oldPublicId: String? = null,
    val message: String
)
