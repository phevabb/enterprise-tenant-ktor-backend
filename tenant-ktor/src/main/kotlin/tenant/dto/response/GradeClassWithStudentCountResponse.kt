package tenant.dto.response



import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GradeClassWithStudentCountResponse(
    val id: Int,
    val name: String,

    @SerialName("is_active")
    val isActive: Boolean,

    val categoryId: Int?,
    val categoryName: String?,
    val studentCount: Int
)