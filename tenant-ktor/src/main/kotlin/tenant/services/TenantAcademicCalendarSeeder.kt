package com.example.tenant.services

import com.example.academics.repos.setTenantSchema
import com.example.student.tables.AcademicYearTable
import com.example.student.tables.TermTable
import com.example.tenant.dto.requests.TenantAcademicCalendarSeed
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

object TenantAcademicCalendarSeeder {

    fun seedAcademicCalendarInSchema(
        tenantSchema: String,
        calendar: TenantAcademicCalendarSeed
    ) {
        val serverNow = System.currentTimeMillis()

        transaction {
            setTenantSchema(tenantSchema)

            // Determine whether this academic year should be current
            val isAcademicYearCurrent =
                serverNow in calendar.startDateEpochMillis..calendar.endDateEpochMillis

            val academicYearId = AcademicYearTable.insert {
                it[name] = calendar.academicYearName
                it[isCurrent] = isAcademicYearCurrent
            } get AcademicYearTable.id

            calendar.terms.forEach { term ->
                val isTermCurrent =
                    serverNow in term.reopeningDateEpochMillis..term.closingDateEpochMillis

                TermTable.insert {
                    it[name] = term.termName
                    it[academic_year] = academicYearId
                    it[isCurrent] = isTermCurrent
                }
            }
        }
    }
}