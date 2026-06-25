package com.example.student.dtos.response






import kotlinx.serialization.Serializable

@Serializable
data class StudentImportError(
    val rowNumber: Int,
    val message: String
)

@Serializable
data class StudentImportResponse(
    val message: String,
    val importedCount: Int,
    val failedCount: Int,
    val errors: List<StudentImportError>
)