package complaints.models


import kotlinx.serialization.Serializable

object ComplaintStatus {
    const val OPEN =
        "OPEN"

    const val IN_REVIEW =
        "IN_REVIEW"

    const val ASSIGNED =
        "ASSIGNED"

    const val AWAITING_PARENT =
        "AWAITING_PARENT"

    const val RESOLVED =
        "RESOLVED"

    const val CLOSED =
        "CLOSED"

    const val REOPENED =
        "REOPENED"

    val allowedValues =
        setOf(
            OPEN,
            IN_REVIEW,
            ASSIGNED,
            AWAITING_PARENT,
            RESOLVED,
            CLOSED,
            REOPENED
        )
}

object ComplaintPriority {
    const val LOW =
        "LOW"

    const val NORMAL =
        "NORMAL"

    const val HIGH =
        "HIGH"

    const val URGENT =
        "URGENT"

    val allowedValues =
        setOf(
            LOW,
            NORMAL,
            HIGH,
            URGENT
        )
}

object ComplaintCategory {
    const val ACADEMIC =
        "ACADEMIC"

    const val FEES =
        "FEES"

    const val TRANSPORT =
        "TRANSPORT"

    const val DISCIPLINE =
        "DISCIPLINE"

    const val HEALTH =
        "HEALTH"

    const val TEACHER_CONDUCT =
        "TEACHER_CONDUCT"

    const val FACILITY =
        "FACILITY"

    const val TECHNICAL =
        "TECHNICAL"

    const val OTHER =
        "OTHER"

    val allowedValues =
        setOf(
            ACADEMIC,
            FEES,
            TRANSPORT,
            DISCIPLINE,
            HEALTH,
            TEACHER_CONDUCT,
            FACILITY,
            TECHNICAL,
            OTHER
        )
}

object ComplaintSenderRole {
    const val PARENT =
        "PARENT"

    const val ADMIN =
        "ADMIN"
}

@Serializable
data class CreateParentComplaintRequest(
    val studentId: Int,
    val category: String,
    val subject: String,
    val description: String,
    val priority: String = ComplaintPriority.NORMAL
)

@Serializable
data class CreateComplaintReplyRequest(
    val content: String
)

@Serializable
data class CreateAdminComplaintReplyRequest(
    val content: String,
    val isInternal: Boolean = false,
    val nextStatus: String? = null
)

@Serializable
data class UpdateComplaintStatusRequest(
    val status: String
)

@Serializable
data class AssignComplaintRequest(
    val adminAccountId: Int?
)

@Serializable
data class ComplaintReplyResponse(
    val id: Int,
    val complaintId: Int,
    val senderAccountId: Int,
    val senderName: String,
    val senderRole: String,
    val content: String,
    val isInternal: Boolean,
    val createdAt: String,
    val readAt: String? = null,
    val isMine: Boolean = false
)

@Serializable
data class ParentComplaintResponse(
    val id: Int,
    val complaintNumber: String,
    val studentId: Int,
    val studentName: String,
    val parentAccountId: Int,
    val category: String,
    val subject: String,
    val description: String,
    val priority: String,
    val status: String,
    val assignedAdminAccountId: Int? = null,
    val assignedAdminName: String? = null,
    val latestReply: String? = null,
    val latestReplyAt: String? = null,
    val unreadReplyCount: Int = 0,
    val createdAt: String,
    val updatedAt: String,
    val resolvedAt: String? = null,
    val closedAt: String? = null,
    val replies: List<ComplaintReplyResponse> = emptyList()
)

@Serializable
data class ComplaintActionResponse(
    val success: Boolean,
    val message: String,
    val complaint: ParentComplaintResponse? = null
)

@Serializable
data class ComplaintReplyActionResponse(
    val success: Boolean,
    val message: String,
    val reply: ComplaintReplyResponse
)