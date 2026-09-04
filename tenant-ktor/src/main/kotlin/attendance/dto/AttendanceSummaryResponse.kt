package attendance.dto



import kotlinx.serialization.Serializable

@Serializable
data class AttendanceSummaryResponse(
    val success: Boolean,
    val totalStudents: Int,
    val present: Int,
    val absent: Int,
    val late: Int,
    val excused: Int,
    val sick: Int,
)