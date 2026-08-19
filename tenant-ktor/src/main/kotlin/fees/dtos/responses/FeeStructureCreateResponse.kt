package fees.dtos.responses



import kotlinx.serialization.Serializable

@Serializable
data class FeeStructureCreateResponse(
    val success: Boolean,
    val message: String,
    val feeStructureId: Int,
    val studentsFound: Int,
    val recordsCreated: Int,
    val recordsSkipped: Int
)