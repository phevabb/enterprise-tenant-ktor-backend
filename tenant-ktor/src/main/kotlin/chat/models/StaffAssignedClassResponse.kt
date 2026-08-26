package chat.models



import kotlinx.serialization.Serializable

@Serializable
data class StaffAssignedClassResponse(
    val staffId: Int,
    val staffAccountId: Int,
    val staffUserId: String,
    val staffName: String,
    val classId: Int,
    val className: String,
    val studentCount: Int,
    val students: List<StaffClassStudentResponse>
)

@Serializable
data class StaffClassStudentResponse(
    val studentId: Int,
    val accountId: Int,
    val userId: String,
    val fullName: String,
    val isActive: Boolean
)