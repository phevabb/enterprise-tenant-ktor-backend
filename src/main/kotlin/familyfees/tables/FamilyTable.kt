package com.example.familyfees.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object FamilyTable: IntIdTable("family_fees_table") {

    val name = varchar("name", 255)
    val is_active = bool("is_active").default(true)


}