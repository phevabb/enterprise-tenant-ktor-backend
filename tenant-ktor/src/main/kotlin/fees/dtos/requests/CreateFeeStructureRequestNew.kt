package fees.dtos.requests



import kotlinx.serialization.Serializable

@Serializable
data class CreateFeeStructureRequest(
    val academicYearId: Int,
    val gradeClassId: Int,
    val termId: Int,
    val amount: Int,
    val isDiscounted: Boolean = false
)