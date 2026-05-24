package com.example.academics.tables




import org.jetbrains.exposed.dao.id.IntIdTable

object SubjectsTable : IntIdTable("subjects") {

    val name = varchar("name", 100)

    // ✅ NEW: direct FK to category
    val category = reference("category_id", CategoriesTable).nullable()
}

