package tenant.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class StudentByClassResponse(
    val id: Int,
    val fullName: String,
    val classId: Int
)