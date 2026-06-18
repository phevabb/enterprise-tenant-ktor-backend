package com.example.academics.repos

import com.example.student.dtos.response.AcademicYearResponse
import com.example.student.tables.AcademicYearTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object NewAcademicYearRepository {

    fun findAll(
        tenantSchema: String
    ): List<AcademicYearResponse> = transaction {

        setTenantSchema(tenantSchema)


        AcademicYearTable
            .selectAll()
            .orderBy(AcademicYearTable.id, SortOrder.DESC)
            .map { row ->
                AcademicYearResponse(
                    id = row[AcademicYearTable.id].value,
                    name = row[AcademicYearTable.name]
                )
            }
    }
}