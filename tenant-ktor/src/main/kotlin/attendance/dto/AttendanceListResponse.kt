package attendance.dto



import kotlinx.serialization.Serializable

@Serializable
data class AttendanceListResponse(
    val success: Boolean,
    val message: String,
    val totalRecords: Int,
    val records: List<AttendanceRecordResponse>,
)