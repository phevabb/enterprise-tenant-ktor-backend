package com.example.academics.repos


import com.example.student.dtos.response.AcademicYearResponse
import com.example.student.dtos.response.TermResponseDto
import com.example.student.tables.AcademicYearTable
import com.example.student.tables.TermTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object NewTermRepository {

    fun findAll(): List<TermResponseDto> = transaction {
        TermTable
            .join(
                AcademicYearTable,
                JoinType.INNER,
                additionalConstraint = { TermTable.academic_year eq AcademicYearTable.id }
            )
            .selectAll()
            .orderBy(TermTable.id, SortOrder.DESC)
            .map { row ->
                TermResponseDto(
                    id = row[TermTable.id].value,
                    name = row[TermTable.name],
                    academic_year = AcademicYearResponse(
                        id = row[AcademicYearTable.id].value,
                        name = row[AcademicYearTable.name]
                    )
                )
            }
    }
}
