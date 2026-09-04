package attendance.table




import attendance.model.AttendanceSession
import attendance.model.AttendanceStatus
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object StudentAttendanceTable : Table("student_attendance") {

    val id = long("id").autoIncrement()

    val schoolId = long("school_id")

    val academicYearId = long("academic_year_id")

    val termId = long("term_id")

    val classId = long("class_id")

    val teacherId = long("teacher_id")

    val studentId = long("student_id")

    val attendanceDate = date("attendance_date")

    val session = enumerationByName(
        name = "session",
        length = 20,
        klass = AttendanceSession::class,
    )

    val status = enumerationByName(
        name = "status",
        length = 20,
        klass = AttendanceStatus::class,
    )

    val arrivalTime = varchar(
        name = "arrival_time",
        length = 8,
    ).nullable()

    val remarks = varchar(
        name = "remarks",
        length = 500,
    ).nullable()

    val generalRemarks = varchar(
        name = "general_remarks",
        length = 500,
    ).nullable()

    val markedBy = long("marked_by").nullable()

    val createdAt = timestamp("created_at")
        .clientDefault {
            Instant.now()
        }

    val updatedAt = timestamp("updated_at")
        .clientDefault {
            Instant.now()
        }

    override val primaryKey = PrimaryKey(
        id,
        name = "pk_student_attendance",
    )

    init {
        uniqueIndex(
            "uq_student_attendance_record",
            schoolId,
            studentId,
            classId,
            attendanceDate,
            session,
        )

        index(
            "idx_attendance_school_date",
            false,
            schoolId,
            attendanceDate,
        )

        index(
            "idx_attendance_class_date",
            false,
            classId,
            attendanceDate,
        )

        index(
            "idx_attendance_student_date",
            false,
            studentId,
            attendanceDate,
        )

        index(
            "idx_attendance_teacher_date",
            false,
            teacherId,
            attendanceDate,
        )

        index(
            "idx_attendance_academic_period",
            false,
            academicYearId,
            termId,
        )
    }
}