package com.example.student.dtos.response

import com.example.student.dtos.response.GradeClassResponse
import com.example.student.tables.NewClassPromotionTable
import com.example.student.tables.NewGradeClassTable
import org.jetbrains.exposed.sql.ResultRow



import org.jetbrains.exposed.sql.Alias


fun ResultRow.toClassPromotionResponseDto(
    currentStageAlias: Alias<NewGradeClassTable>,
    nextStageAlias: Alias<NewGradeClassTable>
): ClassPromotionResponseDto {

    val currentName = this[currentStageAlias[NewGradeClassTable.name]]
    val nextName = this[nextStageAlias[NewGradeClassTable.name]]  // will be null when LEFT JOIN has no match

    val currentId = this[currentStageAlias[NewGradeClassTable.id]].value
    val nextIdEntity = this[nextStageAlias[NewGradeClassTable.id]] // EntityID<Int>? because of LEFT JOIN

    return ClassPromotionResponseDto(
        id = this[NewClassPromotionTable.id].value,

        currentStage = GradeClassResponse(
            id = currentId,
            name = currentName
        ),

        nextStage = nextIdEntity?.let {
            GradeClassResponse(
                id = it.value,
                name = nextName!! // safe because if id exists, name exists too
            )
        },

        promotionPath = "$currentName → ${nextName ?: "Graduated"}"
    )
}
