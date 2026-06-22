package com.example.tenant.dto.requests


import kotlinx.serialization.Serializable

@Serializable
data class CreateTenantRequest(
    val schoolName: String,
    val tenantCode: String,
    val schoolType: String,
    val location: String,
    val contactEmail: String,
    val accountOwnerName: String,
    val primaryDomain: String,
    val academicYear: String,
    val features: List<String>,
    val academicCalendar: TenantAcademicCalendarSeed
)

@Serializable
data class TenantAcademicCalendarSeed(
    val academicYearId: Int,
    val academicYearName: String,
    val startDateEpochMillis: Long,
    val endDateEpochMillis: Long,
    val terms: List<TenantAcademicTermSeed>
)

@Serializable
data class TenantAcademicTermSeed(
    val academicTermId: Int,
    val termCode: String,
    val termName: String,
    val termNumber: Int,
    val reopeningDateEpochMillis: Long,
    val closingDateEpochMillis: Long,
    val vacationStartDateEpochMillis: Long? = null,
    val vacationEndDateEpochMillis: Long? = null,
    val graceStartDateEpochMillis: Long,
    val graceEndDateEpochMillis: Long,
    val paymentDeadlineEpochMillis: Long,
    val amountPerStudentCedis: String
)

