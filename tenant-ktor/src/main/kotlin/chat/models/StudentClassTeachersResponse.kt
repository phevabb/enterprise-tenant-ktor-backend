package chat.models



import kotlinx.serialization.Serializable

@Serializable
data class StudentClassTeachersResponse(
    val studentId: Int,
    val studentAccountId: Int,
    val studentUserId: String,
    val studentName: String,
    val classId: Int,
    val className: String,
    val teachers: List<ClassTeacherResponse>
)

@Serializable
data class ClassTeacherResponse(
    val staffId: Int,
    val accountId: Int,
    val userId: String,
    val fullName: String,
    val role: String,
    val isActive: Boolean
)