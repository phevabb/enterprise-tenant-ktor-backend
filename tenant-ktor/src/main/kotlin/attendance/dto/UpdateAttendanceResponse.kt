package attendance.dto


import kotlinx.serialization.Serializable

@Serializable
data class UpdateAttendanceResponse(
    val success: Boolean,
    val message: String,
    val record: AttendanceRecordResponse? = null,
)