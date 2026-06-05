package com.example.academics.repos

import com.example.academics.tables.SubjectsTable
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object SubjectRepoLite {

    /**
     * Resolve subject by:
     * - numeric string -> id
     * - otherwise -> name (case-insensitive, trim-safe)
     */
    fun findIdByIdOrName(tenantSchema: String, value: String): Int? = transaction {
        setTenantSchema(tenantSchema)
        val clean = value.trim()

        val row = if (clean.all { it.isDigit() }) {
            // ✅ direct ID lookup
            SubjectsTable.selectAll()
                .firstOrNull { it[SubjectsTable.id].value == clean.toInt() }
        } else {
            // ✅ SAFE string comparison (handles spaces + case)
            SubjectsTable.selectAll()
                .firstOrNull {
                    it[SubjectsTable.name].trim().equals(clean, ignoreCase = true)
                }
        }

        row?.get(SubjectsTable.id)?.value
    }
}