package attendance.dto


import kotlinx.serialization.Serializable

@Serializable
data class AttendanceErrorResponse(
    val success: Boolean = false,
    val message: String,
)