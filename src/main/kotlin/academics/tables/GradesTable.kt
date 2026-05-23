package com.example.academics.tables


import org.jetbrains.exposed.dao.id.IntIdTable

object GradesTable : IntIdTable("grades") {
    val code = varchar("code", 1).uniqueIndex()   // ✅ unique
    val label = varchar("label", 50)

    val minScore = integer("min_score")
    val maxScore = integer("max_score")

    val order = integer("order") // ✅ used for sorting
}