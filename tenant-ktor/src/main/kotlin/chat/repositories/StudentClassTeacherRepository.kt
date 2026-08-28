package chat.repositories

import chat.models.ClassTeacherResponse
import chat.models.StudentClassTeachersResponse
import com.example.account.AccountTable
import com.example.academics.repos.setTenantSchema
import com.example.staff.tables.StaffTable
import com.example.student.StudentsTable
import com.example.student.tables.NewGradeClassTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and

import org.jetbrains.exposed.sql.transactions.transaction

object StudentClassTeacherRepository {

    fun findTeachersByStudentUserId(
        tenantSchema: String,
        studentUserId: String
    ): StudentClassTeachersResponse {

        require(tenantSchema.isNotBlank()) {
            "Tenant schema is required."
        }

        val normalizedUserId =
            studentUserId.trim()

        require(normalizedUserId.isNotBlank()) {
            "Student user ID is required."
        }

        return transaction {
            setTenantSchema(
                tenantSchema
            )

            /*
             * Find the student's account.
             */
            val studentAccountRow =
                AccountTable
                    .select(
                        AccountTable.id,
                        AccountTable.userId,
                        AccountTable.fullName,
                        AccountTable.role,
                        AccountTable.isActive
                    )
                    .where {
                        AccountTable.userId eq
                                normalizedUserId
                    }
                    .limit(1)
                    .singleOrNull()
                    ?: throw IllegalArgumentException(
                        "No student account was found with user ID '$normalizedUserId'."
                    )

            val studentAccountId =
                studentAccountRow[
                    AccountTable.id
                ].value

            val studentRole =
                studentAccountRow[
                    AccountTable.role
                ]
                    .trim()
                    .lowercase()

            require(studentRole == "student") {
                "The supplied user ID does not belong to a student."
            }

            val studentName =
                studentAccountRow[
                    AccountTable.fullName
                ]

            /*
             * Find the student profile and current class.
             */
            val studentProfileRow =
                StudentsTable
                    .select(
                        StudentsTable.id,
                        StudentsTable.user,
                        StudentsTable.currentNewGradeClass
                    )
                    .where {
                        StudentsTable.user eq
                                studentAccountId
                    }
                    .limit(1)
                    .singleOrNull()
                    ?: throw IllegalArgumentException(
                        "The student profile was not found."
                    )

            val studentProfileId =
                studentProfileRow[
                    StudentsTable.id
                ].value

            val classId =
                studentProfileRow[
                    StudentsTable.currentNewGradeClass
                ]?.value
                    ?: throw IllegalArgumentException(
                        "$studentName has not been assigned to a current class."
                    )

            /*
             * Find the student's current class.
             */
            val classRow =
                NewGradeClassTable
                    .select(
                        NewGradeClassTable.id,
                        NewGradeClassTable.name,
                        NewGradeClassTable.isActive
                    )
                    .where {
                        NewGradeClassTable.id eq
                                classId
                    }
                    .limit(1)
                    .singleOrNull()
                    ?: throw IllegalArgumentException(
                        "The student's current class was not found."
                    )

            require(
                classRow[
                    NewGradeClassTable.isActive
                ]
            ) {
                "The student's current class is inactive."
            }

            val className =
                classRow[
                    NewGradeClassTable.name
                ]

            /*
             * Find staff profiles assigned to the student's class.
             *
             * StaffTable.user is nullable, so staff profiles
             * without linked accounts are ignored.
             */
            val assignedStaff =
                StaffTable
                    .select(
                        StaffTable.id,
                        StaffTable.user,
                        StaffTable.assignedClass
                    )
                    .where {
                        (StaffTable.assignedClass eq classId) and
                                StaffTable.user.isNotNull()
                    }
                    .mapNotNull { row ->
                        val teacherAccountId =
                            row[
                                StaffTable.user
                            ]?.value
                                ?: return@mapNotNull null

                        AssignedStaffRecord(
                            staffId =
                                row[
                                    StaffTable.id
                                ].value,

                            accountId =
                                teacherAccountId
                        )
                    }
                    .distinctBy { staff ->
                        staff.accountId
                    }

            /*
             * Return student and class information even when
             * no teacher has been assigned yet.
             */
            if (assignedStaff.isEmpty()) {
                return@transaction StudentClassTeachersResponse(
                    studentId =
                        studentProfileId,

                    studentAccountId =
                        studentAccountId,

                    studentUserId =
                        normalizedUserId,

                    studentName =
                        studentName,

                    classId =
                        classId,

                    className =
                        className,

                    teachers =
                        emptyList()
                )
            }

            val staffByAccountId =
                assignedStaff.associateBy { staff ->
                    staff.accountId
                }

            val staffAccountIds =
                assignedStaff
                    .map { staff ->
                        staff.accountId
                    }
                    .distinct()

            /*
             * Load the account details of the assigned teachers.
             */
            val teachers =
                AccountTable
                    .select(
                        AccountTable.id,
                        AccountTable.userId,
                        AccountTable.fullName,
                        AccountTable.role,
                        AccountTable.isActive
                    )
                    .where {
                        (AccountTable.id inList staffAccountIds) and
                                (AccountTable.isActive eq true)
                    }
                    .orderBy(
                        AccountTable.fullName,
                        SortOrder.ASC
                    )
                    .mapNotNull { row ->
                        val teacherAccountId =
                            row[
                                AccountTable.id
                            ].value

                        val staffRecord =
                            staffByAccountId[
                                teacherAccountId
                            ]
                                ?: return@mapNotNull null

                        ClassTeacherResponse(
                            staffId =
                                staffRecord.staffId,

                            accountId =
                                teacherAccountId,

                            userId =
                                row[
                                    AccountTable.userId
                                ],

                            fullName =
                                row[
                                    AccountTable.fullName
                                ],

                            role =
                                row[
                                    AccountTable.role
                                ],

                            isActive =
                                row[
                                    AccountTable.isActive
                                ]
                        )
                    }

            StudentClassTeachersResponse(
                studentId =
                    studentProfileId,

                studentAccountId =
                    studentAccountId,

                studentUserId =
                    normalizedUserId,

                studentName =
                    studentName,

                classId =
                    classId,

                className =
                    className,

                teachers =
                    teachers
            )
        }
    }

    private data class AssignedStaffRecord(
        val staffId: Int,
        val accountId: Int
    )
}