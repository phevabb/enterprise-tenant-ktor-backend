package sms.models

import kotlinx.serialization.Serializable

@Serializable
data class SendAnnouncementRequest(
    val tenantCode: String,
    val audienceType: String,
    val classIds: List<Int> = emptyList(),
    val studentIds: List<Int> = emptyList(),
    val staffIds: List<Int> = emptyList(),
    val customNumbers: List<String> = emptyList(),
    val message: String,
    val description: String? = null
)