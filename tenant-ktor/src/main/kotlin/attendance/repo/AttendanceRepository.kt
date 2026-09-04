package attendance.repo

import attendance.dto.AttendanceRecordResponse
import attendance.dto.SaveAttendanceRequest
import attendance.dto.SaveAttendanceResponse
import attendance.dto.TeacherAttendanceHistoryResponse
import attendance.dto.UpdateAttendanceRequest
import attendance.dto.UpdateAttendanceResponse
import attendance.model.AttendanceSession
import attendance.model.AttendanceStatus
import attendance.table.StudentAttendanceTable
import com.example.config.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.LocalDate
import java.time.Instant




class AttendanceRepository {

    suspend fun getTeacherAttendanceHistory(
        schoolId: Long,
        teacherId: Long,
        fromDate: LocalDate,
        toDate: LocalDate,
        tenantSchema: String,
    ): List<TeacherAttendanceHistoryResponse> {
        require(schoolId > 0) {
            "A valid schoolId is required."
        }

        require(teacherId > 0) {
            "A valid teacherId is required."
        }

        require(!toDate.isBefore(fromDate)) {
            "The end date cannot be before the start date."
        }

        require(
            tenantSchema.matches(
                Regex("^tenant_[a-zA-Z0-9_]+$"),
            ),
        ) {
            "Invalid tenant schema."
        }

        println(
            "[ATTENDANCE HISTORY] schoolId=$schoolId",
        )

        println(
            "[ATTENDANCE HISTORY] teacherId=$teacherId",
        )

        println(
            "[ATTENDANCE HISTORY] fromDate=$fromDate",
        )

        println(
            "[ATTENDANCE HISTORY] toDate=$toDate",
        )

        println(
            "[ATTENDANCE HISTORY] tenantSchema=$tenantSchema",
        )

        return dbQuery(
            tenantSchema = tenantSchema,
        ) {
            StudentAttendanceTable
                .selectAll()
                .where {
                    (
                            StudentAttendanceTable.schoolId eq
                                    schoolId
                            ) and (
                            StudentAttendanceTable.teacherId eq
                                    teacherId
                            ) and (
                            StudentAttendanceTable.attendanceDate
                                .between(
                                    fromDate,
                                    toDate,
                                )
                            )
                }
                .orderBy(
                    StudentAttendanceTable.attendanceDate,
                    SortOrder.DESC,
                )
                .orderBy(
                    StudentAttendanceTable.createdAt,
                    SortOrder.DESC,
                )
                .map { row ->
                    TeacherAttendanceHistoryResponse(
                        id =
                            row[
                                StudentAttendanceTable.id
                            ],

                        schoolId =
                            row[
                                StudentAttendanceTable.schoolId
                            ],

                        academicYearId =
                            row[
                                StudentAttendanceTable
                                    .academicYearId
                            ],

                        termId =
                            row[
                                StudentAttendanceTable.termId
                            ],

                        classId =
                            row[
                                StudentAttendanceTable.classId
                            ],

                        teacherId =
                            row[
                                StudentAttendanceTable.teacherId
                            ],

                        studentId =
                            row[
                                StudentAttendanceTable.studentId
                            ],

                        attendanceDate =
                            row[
                                StudentAttendanceTable
                                    .attendanceDate
                            ].toString(),

                        session =
                            row[
                                StudentAttendanceTable.session
                            ].name,

                        status =
                            row[
                                StudentAttendanceTable.status
                            ].name,

                        arrivalTime =
                            row[
                                StudentAttendanceTable.arrivalTime
                            ],

                        remarks =
                            row[
                                StudentAttendanceTable.remarks
                            ],

                        generalRemarks =
                            row[
                                StudentAttendanceTable
                                    .generalRemarks
                            ],

                        markedBy =
                            row[
                                StudentAttendanceTable.markedBy
                            ],

                        createdAt =
                            row[
                                StudentAttendanceTable.createdAt
                            ].toString(),

                        updatedAt =
                            row[
                                StudentAttendanceTable.updatedAt
                            ].toString(),
                    )
                }
        }
    }





    suspend fun updateAttendanceRecord(
        attendanceId: Long,
        request: UpdateAttendanceRequest,
        tenantSchema: String,
    ): UpdateAttendanceResponse {
        return dbQuery(
            tenantSchema = tenantSchema,
        ) attendanceQuery@{
            val existingRecord =
                StudentAttendanceTable
                    .selectAll()
                    .where {
                        StudentAttendanceTable.id eq
                                attendanceId
                    }
                    .limit(1)
                    .singleOrNull()

            if (existingRecord == null) {
                return@attendanceQuery UpdateAttendanceResponse(
                    success = false,
                    message = "Attendance record not found",
                    record = null,
                )
            }

            val existingStatus =
                existingRecord[
                    StudentAttendanceTable.status
                ]

            val selectedStatus =
                request.status
                    ?.trim()
                    ?.takeIf { value ->
                        value.isNotEmpty()
                    }
                    ?.let { value ->
                        try {
                            AttendanceStatus.valueOf(
                                value.uppercase(),
                            )
                        } catch (
                            error: IllegalArgumentException
                        ) {
                            throw IllegalArgumentException(
                                "Invalid attendance status: $value",
                            )
                        }
                    }
                    ?: existingStatus

            val updatedArrivalTime =
                if (
                    selectedStatus ==
                    AttendanceStatus.LATE
                ) {
                    request.arrivalTime
                        ?.trim()
                        ?.takeIf { value ->
                            value.isNotEmpty()
                        }
                        ?: existingRecord[
                            StudentAttendanceTable.arrivalTime
                        ]
                } else {
                    null
                }

            if (selectedStatus == AttendanceStatus.LATE) {
                require(
                    !updatedArrivalTime.isNullOrBlank(),
                ) {
                    "Arrival time is required when attendance status is LATE."
                }

                require(
                    Regex(
                        "^([01][0-9]|2[0-3]):[0-5][0-9]$",
                    ).matches(updatedArrivalTime),
                ) {
                    "Arrival time must use HH:mm format."
                }
            }

            val updatedRows =
                StudentAttendanceTable.update(
                    where = {
                        StudentAttendanceTable.id eq
                                attendanceId
                    },
                ) { statement ->
                    statement[
                        StudentAttendanceTable.status
                    ] = selectedStatus

                    statement[
                        StudentAttendanceTable.arrivalTime
                    ] = updatedArrivalTime

                    if (request.remarks != null) {
                        statement[
                            StudentAttendanceTable.remarks
                        ] = request.remarks
                            .trim()
                            .takeIf { value ->
                                value.isNotEmpty()
                            }
                    }

                    if (request.generalRemarks != null) {
                        statement[
                            StudentAttendanceTable.generalRemarks
                        ] = request.generalRemarks
                            .trim()
                            .takeIf { value ->
                                value.isNotEmpty()
                            }
                    }

                    if (request.markedBy != null) {
                        statement[
                            StudentAttendanceTable.markedBy
                        ] = request.markedBy
                    }

                    statement[
                        StudentAttendanceTable.updatedAt
                    ] = java.time.Instant.now()
                }

            if (updatedRows == 0) {
                return@attendanceQuery UpdateAttendanceResponse(
                    success = false,
                    message = "Attendance record was not updated",
                    record = null,
                )
            }

            val updatedRecord =
                StudentAttendanceTable
                    .selectAll()
                    .where {
                        StudentAttendanceTable.id eq
                                attendanceId
                    }
                    .limit(1)
                    .singleOrNull()

            UpdateAttendanceResponse(
                success = true,
                message =
                    "Attendance record updated successfully",
                record =
                    updatedRecord?.let { row ->
                        mapAttendanceRecord(row)
                    },
            )
        }
    }
//    suspend fun replaceClassAttendance(
//        request: SaveAttendanceRequest,
//        markedBy: Long?,
//    ): SaveAttendanceResponse {
//        return saveAttendance(
//            request = request,
//            markedBy = markedBy,
//        )
//    }




    suspend fun saveAttendance(
        request: SaveAttendanceRequest,
        schoolId: Int,
        tenantSchema: String,
        markedBy: Long?,
    ): SaveAttendanceResponse {
        require(schoolId > 0) {
            "A valid schoolId is required."
        }

        val teacherId =
            requireNotNull(request.teacherId) {
                "A valid teacherId is required."
            }

        require(teacherId > 0) {
            "A valid teacherId is required."
        }

        val classId =
            requireNotNull(request.classId) {
                "A valid classId is required."
            }

        require(classId > 0) {
            "A valid classId is required."
        }

        val academicYearId =
            requireNotNull(request.academicYearId) {
                "A valid academicYearId is required."
            }

        require(academicYearId > 0) {
            "A valid academicYearId is required."
        }

        val termId =
            requireNotNull(request.termId) {
                "A valid termId is required."
            }

        require(termId > 0) {
            "A valid termId is required."
        }

        require(request.students.isNotEmpty()) {
            "At least one student attendance record is required."
        }

        val attendanceDate =
            try {
                LocalDate.parse(
                    request.attendanceDate.trim(),
                )
            } catch (error: Exception) {
                throw IllegalArgumentException(
                    "Attendance date must be in YYYY-MM-DD format.",
                )
            }

        val attendanceSession =
            try {
                AttendanceSession.valueOf(
                    request.session
                        .trim()
                        .uppercase(),
                )
            } catch (error: Exception) {
                throw IllegalArgumentException(
                    "Invalid attendance session.",
                )
            }

        val generalRemarks =
            request.remarks
                ?.trim()
                ?.takeIf { it.isNotEmpty() }

        require(
            generalRemarks == null ||
                    generalRemarks.length <= 500
        ) {
            "General remarks cannot exceed 500 characters."
        }

        println(
            "[ATTENDANCE REPOSITORY] schoolId=$schoolId",
        )

        println(
            "[ATTENDANCE REPOSITORY] teacherId=$teacherId",
        )

        println(
            "[ATTENDANCE REPOSITORY] classId=$classId",
        )

        return dbQuery(

                    tenantSchema = tenantSchema,

        ){
            var insertedRecords = 0
            var updatedRecords = 0

            request.students.forEach { student ->
                require(student.studentId > 0) {
                    "A valid studentId is required."
                }

                val attendanceStatus =
                    try {
                        AttendanceStatus.valueOf(
                            student.status
                                .trim()
                                .uppercase(),
                        )
                    } catch (error: Exception) {
                        throw IllegalArgumentException(
                            "Invalid attendance status for student ${student.studentId}.",
                        )
                    }

                val arrivalTime =
                    if (
                        attendanceStatus ==
                        AttendanceStatus.LATE
                    ) {
                        val time =
                            student.arrivalTime
                                ?.trim()
                                ?.takeIf {
                                    it.isNotEmpty()
                                }

                        require(time != null) {
                            "Arrival time is required for late student ${student.studentId}."
                        }

                        require(
                            Regex(
                                "^([01][0-9]|2[0-3]):[0-5][0-9]$",
                            ).matches(time)
                        ) {
                            "Invalid arrival time for student ${student.studentId}."
                        }

                        time
                    } else {
                        null
                    }

                val studentRemarks =
                    student.remarks
                        ?.trim()
                        ?.takeIf {
                            it.isNotEmpty()
                        }

                require(
                    studentRemarks == null ||
                            studentRemarks.length <= 500
                ) {
                    "Remarks for student ${student.studentId} cannot exceed 500 characters."
                }

                val existingRecord =
                    StudentAttendanceTable
                        .selectAll()
                        .where {
                            (
                                    StudentAttendanceTable.schoolId eq
                                            schoolId.toLong()
                                    ) and (
                                    StudentAttendanceTable.studentId eq
                                            student.studentId
                                    ) and (
                                    StudentAttendanceTable.classId eq
                                            classId
                                    ) and (
                                    StudentAttendanceTable.attendanceDate eq
                                            attendanceDate
                                    ) and (
                                    StudentAttendanceTable.session eq
                                            attendanceSession
                                    )
                        }
                        .limit(1)
                        .singleOrNull()

                if (existingRecord == null) {
                    StudentAttendanceTable.insert {
                        it[
                            StudentAttendanceTable.schoolId
                        ] = schoolId.toLong()

                        it[
                            StudentAttendanceTable.academicYearId
                        ] = academicYearId

                        it[
                            StudentAttendanceTable.termId
                        ] = termId

                        it[
                            StudentAttendanceTable.classId
                        ] = classId

                        it[
                            StudentAttendanceTable.teacherId
                        ] = teacherId

                        it[
                            StudentAttendanceTable.studentId
                        ] = student.studentId

                        it[
                            StudentAttendanceTable.attendanceDate
                        ] = attendanceDate

                        it[
                            StudentAttendanceTable.session
                        ] = attendanceSession

                        it[
                            StudentAttendanceTable.status
                        ] = attendanceStatus

                        it[
                            StudentAttendanceTable.arrivalTime
                        ] = arrivalTime

                        it[
                            StudentAttendanceTable.remarks
                        ] = studentRemarks

                        it[
                            StudentAttendanceTable.generalRemarks
                        ] = generalRemarks

                        it[
                            StudentAttendanceTable.markedBy
                        ] = markedBy

                        it[
                            StudentAttendanceTable.createdAt
                        ] = Instant.now()

                        it[
                            StudentAttendanceTable.updatedAt
                        ] = Instant.now()
                    }

                    insertedRecords++
                } else {
                    val attendanceId =
                        existingRecord[
                            StudentAttendanceTable.id
                        ]

                    StudentAttendanceTable.update(
                        where = {
                            StudentAttendanceTable.id eq
                                    attendanceId
                        },
                    ) {
                        it[
                            StudentAttendanceTable.academicYearId
                        ] = academicYearId

                        it[
                            StudentAttendanceTable.termId
                        ] = termId

                        it[
                            StudentAttendanceTable.teacherId
                        ] = teacherId

                        it[
                            StudentAttendanceTable.status
                        ] = attendanceStatus

                        it[
                            StudentAttendanceTable.arrivalTime
                        ] = arrivalTime

                        it[
                            StudentAttendanceTable.remarks
                        ] = studentRemarks

                        it[
                            StudentAttendanceTable.generalRemarks
                        ] = generalRemarks

                        it[
                            StudentAttendanceTable.markedBy
                        ] = markedBy

                        it[
                            StudentAttendanceTable.updatedAt
                        ] = Instant.now()
                    }

                    updatedRecords++
                }
            }

            println(
                "[ATTENDANCE REPOSITORY] inserted=$insertedRecords",
            )

            println(
                "[ATTENDANCE REPOSITORY] updated=$updatedRecords",
            )

            SaveAttendanceResponse(
                success = true,
                message =
                    "Attendance submitted successfully. " +
                            "$insertedRecords record(s) added and " +
                            "$updatedRecords record(s) updated.",
            )
        }
    }

    suspend fun getClassAttendance(
        schoolId: Long,
        classId: Long,
        attendanceDate: LocalDate,
        session: AttendanceSession,
    ): List<AttendanceRecordResponse> {
        return newSuspendedTransaction {
            StudentAttendanceTable
                .selectAll()
                .where {
                    StudentAttendanceTable.schoolId
                        .eq(schoolId)
                        .and(
                            StudentAttendanceTable.classId
                                .eq(classId),
                        )
                        .and(
                            StudentAttendanceTable.attendanceDate
                                .eq(attendanceDate),
                        )
                        .and(
                            StudentAttendanceTable.session
                                .eq(session),
                        )
                }
                .orderBy(
                    StudentAttendanceTable.studentId,
                )
                .map { row ->
                    mapAttendanceRecord(row)
                }
        }
    }

    suspend fun getStudentAttendance(
        schoolId: Long,
        studentId: Long,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<AttendanceRecordResponse> {
        return newSuspendedTransaction {
            StudentAttendanceTable
                .selectAll()
                .where {
                    StudentAttendanceTable.schoolId
                        .eq(schoolId)
                        .and(
                            StudentAttendanceTable.studentId
                                .eq(studentId),
                        )
                        .and(
                            StudentAttendanceTable.attendanceDate
                                .greaterEq(startDate),
                        )
                        .and(
                            StudentAttendanceTable.attendanceDate
                                .lessEq(endDate),
                        )
                }
                .orderBy(
                    StudentAttendanceTable.attendanceDate,
                )
                .map { row ->
                    mapAttendanceRecord(row)
                }
        }
    }

    suspend fun deleteClassAttendance(
        schoolId: Long,
        classId: Long,
        attendanceDate: LocalDate,
        session: AttendanceSession,
        tenantSchema: String,
    ): Int {
        return dbQuery(
            tenantSchema = tenantSchema,
        ) {
            StudentAttendanceTable.deleteWhere {
                (StudentAttendanceTable.schoolId eq schoolId) and
                        (StudentAttendanceTable.classId eq classId) and
                        (StudentAttendanceTable.attendanceDate eq attendanceDate) and
                        (StudentAttendanceTable.session eq session)
            }
        }
    }

    private fun requireLongId(
        value: Any?,
        fieldName: String,
    ): Long {
        val convertedValue = when (value) {
            is Long -> value
            is Int -> value.toLong()
            is Short -> value.toLong()
            is String -> value.trim().toLongOrNull()
            else -> null
        }

        return convertedValue
            ?.takeIf { id -> id > 0 }
            ?: throw IllegalArgumentException(
                "A valid $fieldName is required",
            )
    }

    private fun cleanText(
        value: String?,
    ): String? {
        return value
            ?.trim()
            ?.takeIf { text ->
                text.isNotEmpty()
            }
    }

    private fun getArrivalTime(
        selectedStatus: AttendanceStatus,
        arrivalTimeValue: String?,
    ): String? {
        if (selectedStatus != AttendanceStatus.LATE) {
            return null
        }

        return cleanText(arrivalTimeValue)
    }

    private fun mapAttendanceRecord(
        row: ResultRow,
    ): AttendanceRecordResponse {
        return AttendanceRecordResponse(
            id = row[StudentAttendanceTable.id],
            schoolId = row[StudentAttendanceTable.schoolId],
            academicYearId =
                row[StudentAttendanceTable.academicYearId],
            termId = row[StudentAttendanceTable.termId],
            classId = row[StudentAttendanceTable.classId],
            teacherId = row[StudentAttendanceTable.teacherId],
            studentId = row[StudentAttendanceTable.studentId],
            attendanceDate =
                row[StudentAttendanceTable.attendanceDate]
                    .toString(),
            session =
                row[StudentAttendanceTable.session].name,
            status =
                row[StudentAttendanceTable.status].name,
            arrivalTime =
                row[StudentAttendanceTable.arrivalTime],
            remarks =
                row[StudentAttendanceTable.remarks],
            generalRemarks =
                row[StudentAttendanceTable.generalRemarks],
            markedBy =
                row[StudentAttendanceTable.markedBy],
            createdAt =
                row[StudentAttendanceTable.createdAt]
                    .toString(),
            updatedAt =
                row[StudentAttendanceTable.updatedAt]
                    .toString(),
        )
    }
}