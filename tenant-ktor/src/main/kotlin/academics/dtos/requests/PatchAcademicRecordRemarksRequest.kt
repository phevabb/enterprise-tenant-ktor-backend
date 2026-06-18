package com.example.academics.dtos.requests

import kotlinx.serialization.Serializable

@Serializable
data class PatchAcademicRecordRemarksRequest(
    val attendance: String? = null,
    val numberOnRoll: Int? = null,
    val promotedTo: String? = null,

    val conduct: String? = null,
    val interest: String? = null,
    val attitude: String? = null,

    val teacherRemarks: String? = null,
    val headTeacherRemarks: String? = null,
    val nextTermBegins: String? = null
)
