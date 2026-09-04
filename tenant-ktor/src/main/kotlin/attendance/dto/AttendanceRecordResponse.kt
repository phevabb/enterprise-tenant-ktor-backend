package attendance.dto


import kotlinx.serialization.Serializable

@Serializable
data class AttendanceRecordResponse(
    val id: Long,
    val schoolId: Long,
    val academicYearId: Long,
    val termId: Long,
    val classId: Long,
    val teacherId: Long,
    val studentId: Long,
    val attendanceDate: String,
    val session: String,
    val status: String,
    val arrivalTime: String? = null,
    val remarks: String? = null,
    val generalRemarks: String? = null,
    val markedBy: Long? = null,
    val createdAt: String,
    val updatedAt: String,
)