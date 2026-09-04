package attendance.service

import attendance.dto.AttendanceListResponse
import attendance.dto.AttendanceSummaryResponse
import attendance.dto.SaveAttendanceRequest
import attendance.dto.SaveAttendanceResponse
import attendance.dto.TeacherAttendanceHistoryResponse
import attendance.dto.UpdateAttendanceRequest
import attendance.dto.UpdateAttendanceResponse
import attendance.model.AttendanceSession
import attendance.model.AttendanceStatus
import attendance.repo.AttendanceRepository

import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlin.require

class AttendanceService(
    private val attendanceRepository: AttendanceRepository,
) {



    suspend fun getTeacherAttendanceHistory(
        schoolId: Long,
        teacherId: Long,
        fromDate: LocalDate,
        toDate: LocalDate,
        tenantSchema: String,
    ): List<TeacherAttendanceHistoryResponse> {
        return attendanceRepository
            .getTeacherAttendanceHistory(
                schoolId = schoolId,
                teacherId = teacherId,
                fromDate = fromDate,
                toDate = toDate,
                tenantSchema = tenantSchema,
            )
    }
    suspend fun updateAttendanceRecord(
        attendanceId: Long,
        request: UpdateAttendanceRequest,
        tenantSchema: String,
    ): UpdateAttendanceResponse {
        return attendanceRepository.updateAttendanceRecord(
            attendanceId = attendanceId,
            request = request,
            tenantSchema = tenantSchema,
        )
    }

//    suspend fun replaceClassAttendance(
//        request: SaveAttendanceRequest,
//        markedBy: Long?,
//    ): SaveAttendanceResponse {
//        validateSaveRequest(request)
//
//        return attendanceRepository.replaceClassAttendance(
//            request = request,
//            markedBy = markedBy,
//        )
//    }




    suspend fun saveAttendance(
        request: SaveAttendanceRequest,
        schoolId: Int,
        markedBy: Long?,
        tenantSchema: String,
    ): SaveAttendanceResponse {
        validateSaveRequest(
            request = request,
            schoolId = schoolId,
        )

        return attendanceRepository.saveAttendance(
            request = request,
            schoolId = schoolId,
            markedBy = markedBy,
            tenantSchema = tenantSchema,
        )
    }









    suspend fun getClassAttendance(
        schoolId: Long,
        classId: Long,
        attendanceDateValue: String,
        sessionValue: String,
    ): AttendanceListResponse {
        require(schoolId > 0) {
            "A valid school ID is required"
        }

        require(classId > 0) {
            "A valid class ID is required"
        }

        val attendanceDate = parseDate(
            attendanceDateValue,
        )

        val attendanceSession = parseSession(
            sessionValue,
        )

        val records =
            attendanceRepository.getClassAttendance(
                schoolId = schoolId,
                classId = classId,
                attendanceDate = attendanceDate,
                session = attendanceSession,
            )

        return AttendanceListResponse(
            success = true,
            message = if (records.isEmpty()) {
                "No attendance records found"
            } else {
                "Attendance records loaded successfully"
            },
            totalRecords = records.size,
            records = records,
        )
    }

    suspend fun getStudentAttendance(
        schoolId: Long,
        studentId: Long,
        startDateValue: String,
        endDateValue: String,
    ): AttendanceListResponse {
        require(schoolId > 0) {
            "A valid school ID is required"
        }

        require(studentId > 0) {
            "A valid student ID is required"
        }

        val startDate = parseDate(startDateValue)
        val endDate = parseDate(endDateValue)

        require(!endDate.isBefore(startDate)) {
            "The end date cannot be before the start date"
        }

        val records =
            attendanceRepository.getStudentAttendance(
                schoolId = schoolId,
                studentId = studentId,
                startDate = startDate,
                endDate = endDate,
            )

        return AttendanceListResponse(
            success = true,
            message = if (records.isEmpty()) {
                "No attendance records found"
            } else {
                "Student attendance loaded successfully"
            },
            totalRecords = records.size,
            records = records,
        )
    }

    suspend fun getClassAttendanceSummary(
        schoolId: Long,
        classId: Long,
        attendanceDateValue: String,
        sessionValue: String,
    ): AttendanceSummaryResponse {
        val result = getClassAttendance(
            schoolId = schoolId,
            classId = classId,
            attendanceDateValue = attendanceDateValue,
            sessionValue = sessionValue,
        )

        return AttendanceSummaryResponse(
            success = true,
            totalStudents = result.records.size,
            present = result.records.count {
                it.status == AttendanceStatus.PRESENT.name
            },
            absent = result.records.count {
                it.status == AttendanceStatus.ABSENT.name
            },
            late = result.records.count {
                it.status == AttendanceStatus.LATE.name
            },
            excused = result.records.count {
                it.status == AttendanceStatus.EXCUSED.name
            },
            sick = result.records.count {
                it.status == AttendanceStatus.SICK.name
            },
        )
    }

    suspend fun deleteClassAttendance(
        schoolId: Long,
        classId: Long,
        attendanceDateValue: String,
        sessionValue: String,
        tenantSchema: String,
    ): Int {
        val attendanceDate =
            LocalDate.parse(attendanceDateValue)

        val session =
            AttendanceSession.valueOf(
                sessionValue.trim().uppercase(),
            )

        return attendanceRepository.deleteClassAttendance(
            schoolId = schoolId,
            classId = classId,
            attendanceDate = attendanceDate,
            session = session,
            tenantSchema = tenantSchema,
        )
    }

    private fun validateSaveRequest(
        request: SaveAttendanceRequest,
        schoolId: Int,
    ) {
        require(schoolId > 0) {
            "A valid schoolId is required."
        }

        request.teacherId?.let {
            require(it > 0) {
                "A valid teacherId is required."
            }
        }

        request.classId?.let {
            require(it > 0) {
                "A valid classId is required."
            }
        }

        request.academicYearId?.let {
            require(it > 0) {
                "A valid academicYearId is required."
            }
        }

        request.termId?.let {
            require(it > 0) {
                "A valid termId is required."
            }
        }

        require(request.students.isNotEmpty()) {
            "At least one student attendance record is required."
        }
    }

    private fun parseDate(
        dateValue: String,
    ): LocalDate {
        try {
            return LocalDate.parse(dateValue)
        } catch (error: DateTimeParseException) {
            throw IllegalArgumentException(
                "Attendance dates must use yyyy-MM-dd format",
            )
        }
    }

    private fun parseStatus(
        statusValue: String,
    ): AttendanceStatus {
        try {
            return AttendanceStatus.valueOf(
                statusValue.trim().uppercase(),
            )
        } catch (error: IllegalArgumentException) {
            throw IllegalArgumentException(
                "Invalid attendance status: $statusValue",
            )
        }
    }

    private fun parseSession(
        sessionValue: String,
    ): AttendanceSession {
        try {
            return AttendanceSession.valueOf(
                sessionValue.trim().uppercase(),
            )
        } catch (error: IllegalArgumentException) {
            throw IllegalArgumentException(
                "Invalid attendance session: $sessionValue",
            )
        }
    }
}