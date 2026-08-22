package sms.dto

import kotlinx.serialization.Serializable

@Serializable
data class ClientSenderIdResponse(
    val available: Boolean,
    val id: Int? = null,
    val tenantCode: String,
    val schoolName: String? = null,
    val senderId: String? = null,
    val status: String = "not_requested",
    val rejectionReason: String? = null,
    val requestedAt: String? = null,
    val approvedAt: String? = null
)