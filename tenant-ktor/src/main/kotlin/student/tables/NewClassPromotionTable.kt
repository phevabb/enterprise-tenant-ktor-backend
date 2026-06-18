package com.example.student.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object NewClassPromotionTable : IntIdTable("new_class_promotions") {
    val currentStageId = reference(
        "current_stage",
        NewGradeClassTable
    ).uniqueIndex()

    val nextStageId = reference(
        "next_stage",
        NewGradeClassTable
    ).nullable()
}