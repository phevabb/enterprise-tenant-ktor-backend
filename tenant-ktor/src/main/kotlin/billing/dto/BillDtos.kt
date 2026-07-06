package com.example.billing.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateBillTemplateRequest(
    val name: String,
    val categoryId: Int,
    val academicYearId: Int,
    val academicTermId: Int,
    val description: String? = null,
    val items: List<CreateBillTemplateItemRequest> = emptyList()
)

@Serializable
data class CreateBillTemplateItemRequest(
    val itemName: String,
    val description: String? = null,
    val amountCedis: Double? = null,
    val itemType: String = "fixed",
    val sortOrder: Int = 0,
    val isActive: Boolean = true
)

@Serializable
data class UpdateBillTemplateRequest(
    val name: String? = null,
    val description: String? = null,
    val isActive: Boolean? = null
)

@Serializable
data class UpdateBillTemplateItemRequest(
    val itemName: String? = null,
    val description: String? = null,
    val amountCedis: Double? = null,
    val itemType: String? = null,
    val sortOrder: Int? = null,
    val isActive: Boolean? = null
)

@Serializable
data class GenerateIndividualBillsRequest(
    val dueDateEpochMillis: Long? = null
)

@Serializable
data class UpdateStudentBillStatusRequest(
    val status: String
)

@Serializable
data class RecordStudentBillPaymentRequest(
    val amountPaidCedis: Double
)

@Serializable
data class BillTemplateItemResponse(
    val id: Int,
    val itemName: String,
    val description: String?,
    val amountCedis: Double?,
    val itemType: String,
    val sortOrder: Int,
    val isActive: Boolean
)

@Serializable
data class BillTemplateResponse(
    val id: Int,
    val name: String,

    val categoryId: Int,
    val categoryName: String?,

    val academicYearId: Int,
    val academicYearName: String?,

    val academicTermId: Int,
    val academicTermName: String?,

    val description: String?,
    val isActive: Boolean,

    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long?,

    val items: List<BillTemplateItemResponse> = emptyList()
)

@Serializable
data class GeneratedBillSummaryResponse(
    val generatedCount: Int,
    val skippedCount: Int,
    val generatedBillIds: List<Int>,
    val skippedStudents: List<SkippedStudentBillResponse>
)

@Serializable
data class SkippedStudentBillResponse(
    val studentId: Int,
    val studentName: String?,
    val reason: String
)

@Serializable
data class StudentBillItemResponse(
    val id: Int,
    val itemName: String,
    val description: String?,
    val amountCedis: Double,
    val itemType: String,
    val sortOrder: Int
)

@Serializable
data class StudentBillResponse(
    val id: Int,

    val studentId: Int,
    val studentName: String?,

    val billTemplateId: Int,
    val billTemplateName: String?,

    val categoryId: Int?,
    val categoryName: String?,

    val academicYearId: Int,
    val academicYearName: String?,

    val academicTermId: Int,
    val academicTermName: String?,

    val billNumber: String,
    val classNameSnapshot: String?,

    val subTotalCedis: Double,
    val arrearsCedis: Double,
    val discountCedis: Double,
    val totalAmountCedis: Double,
    val amountPaidCedis: Double,
    val balanceCedis: Double,

    val status: String,
    val dueDateEpochMillis: Long?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long?,

    val items: List<StudentBillItemResponse> = emptyList()
)