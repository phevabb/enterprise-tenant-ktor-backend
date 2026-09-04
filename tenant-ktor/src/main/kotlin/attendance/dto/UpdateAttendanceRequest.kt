package attendance.dto



import kotlinx.serialization.Serializable

@Serializable
data class UpdateAttendanceRequest(
    val status: String? = null,
    val arrivalTime: String? = null,
    val remarks: String? = null,
    val generalRemarks: String? = null,
    val markedBy: Long? = null,
)