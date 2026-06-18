package com.example.student.mappers
import com.example.student.tables.NewClassPromotionTable

import com.example.student.models.NewClassPromotionModel
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toNewClassPromotionModel() =
    NewClassPromotionModel(
        id = this[NewClassPromotionTable.id].value,
        currentStageId = this[NewClassPromotionTable.currentStageId].value,
        nextStageId = this[NewClassPromotionTable.nextStageId]?.value
    )