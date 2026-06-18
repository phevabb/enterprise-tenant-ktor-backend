package com.example.fees.notifications



import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MnotifySmsRequest(
    val recipient: List<String>,
    val sender: String,
    val message: String,

    @SerialName("is_schedule")
    val isSchedule: Boolean = false,

    @SerialName("schedule_date")
    val scheduleDate: String = ""
)