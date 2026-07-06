package com.example.billing.dto



import kotlinx.serialization.Serializable

@Serializable
data class CategoryLookupResponse(
    val id: Int,
    val name: String
)

@Serializable
data class AcademicYearLookupResponse(
    val id: Int,
    val name: String,
    val isCurrent: Boolean
)

@Serializable
data class AcademicTermLookupResponse(
    val id: Int,
    val name: String,
    val academicYearId: Int,
    val isCurrent: Boolean
)

@Serializable
data class BillingTemplateLookupsResponse(
    val categories: List<CategoryLookupResponse>,
    val academicYears: List<AcademicYearLookupResponse>,
    val academicTerms: List<AcademicTermLookupResponse>,
    val currentAcademicYear: AcademicYearLookupResponse?,
    val currentAcademicTerm: AcademicTermLookupResponse?
)