package attendance.dto



import kotlinx.serialization.Serializable

@Serializable
data class SaveAttendanceResponse(
    val success: Boolean,
    val message: String,
    val attendanceDate: String? = null,
    val session: String? = null,
    val totalStudents: Int = 0,
    val insertedRecords: Int = 0,
    val updatedRecords: Int = 0,
)