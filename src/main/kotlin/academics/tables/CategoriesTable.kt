package com.example.academics.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object CategoriesTable : IntIdTable("categories") {
    val name = varchar("name", 100).uniqueIndex()
}
