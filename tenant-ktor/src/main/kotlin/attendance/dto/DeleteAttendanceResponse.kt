package attendance.dto



import kotlinx.serialization.Serializable

@Serializable
data class DeleteAttendanceResponse(
    val success: Boolean,
    val message: String,
    val deletedRecords: Int,
)