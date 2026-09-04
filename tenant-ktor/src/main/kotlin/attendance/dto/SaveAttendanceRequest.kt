package attendance.dto



import kotlinx.serialization.Serializable

@Serializable
data class SaveAttendanceRequest(
    val teacherId: Long? = null,
    val classId: Long? = null,
    val academicYearId: Long? = null,
    val termId: Long? = null,
    val attendanceDate: String,
    val session: String,
    val remarks: String? = null,
    val schoolId: String? = null,
    val students: List<StudentAttendanceRequest>,
)

@Serializable
data class StudentAttendanceRequest(
    val studentId: Long,
    val indexNo: String? = null,
    val status: String,
    val arrivalTime: String? = null,
    val remarks: String? = null,
)