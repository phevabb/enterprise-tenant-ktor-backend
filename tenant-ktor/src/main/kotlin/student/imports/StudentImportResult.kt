package student.imports


import kotlinx.serialization.Serializable

@Serializable
data class StudentImportError(
    val rowNumber: Int,
    val message: String
)

@Serializable
data class StudentImportResult(
    val importedCount: Int,
    val failedCount: Int,
    val message: String,
    val errors: List<StudentImportError>
)