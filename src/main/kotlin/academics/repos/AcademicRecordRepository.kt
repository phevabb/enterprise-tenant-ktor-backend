package com.example.academics.repos

import com.example.academics.dtos.response.AcademicRecordResponse
import com.example.academics.mappers.toAcademicRecord
import com.example.academics.tables.AcademicRecordsTable



import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object AcademicRecordRepository {

    fun getOrCreate(
        studentId: Int,
        termId: Int,
        academicYearId: Int,
        classLevelId: Int
    ): AcademicRecordResponse = transaction {

        val existing = AcademicRecordsTable
            .selectAll()
            .where {
                (AcademicRecordsTable.student eq studentId) and
                        (AcademicRecordsTable.term eq termId) and
                        (AcademicRecordsTable.academicYear eq academicYearId)
            }
            .singleOrNull()

        if (existing != null) {
            // ✅ Update class if changed
            if (existing[AcademicRecordsTable.classLevel].value != classLevelId) {
                AcademicRecordsTable.update(
                    { AcademicRecordsTable.id eq existing[AcademicRecordsTable.id] }
                ) {
                    it[classLevel] = classLevelId
                }
            }
            return@transaction existing.toAcademicRecord()
        }

        val newId = AcademicRecordsTable.insertAndGetId {
            it[student] = studentId
            it[term] = termId
            it[academicYear] = academicYearId
            it[classLevel] = classLevelId
        }.value

        AcademicRecordsTable
            .selectAll()
            .where { AcademicRecordsTable.id eq newId }
            .single()
            .toAcademicRecord()
    }

    fun findAll(): List<AcademicRecordResponse> = transaction {
        AcademicRecordsTable.selectAll().map { it.toAcademicRecord() }
    }
}
