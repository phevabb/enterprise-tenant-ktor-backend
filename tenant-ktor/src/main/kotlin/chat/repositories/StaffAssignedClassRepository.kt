package chat.repositories

import chat.models.StaffAssignedClassResponse
import chat.models.StaffClassStudentResponse
import com.example.account.AccountTable
import com.example.academics.repos.setTenantSchema
import com.example.staff.tables.StaffTable
import com.example.student.StudentsTable
import com.example.student.tables.NewGradeClassTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object StaffAssignedClassRepository {

    fun findAssignedClassAndStudents(
        tenantSchema: String,
        authenticatedAccountId: Int,
        staffUserId: String
    ): StaffAssignedClassResponse {

        require(tenantSchema.isNotBlank()) {
            "Tenant schema is required."
        }

        require(authenticatedAccountId > 0) {
            "A valid authenticated account ID is required."
        }

        val normalizedStaffUserId =
            staffUserId.trim()

        require(normalizedStaffUserId.isNotBlank()) {
            "Staff user ID is required."
        }

        return transaction {
            setTenantSchema(
                tenantSchema
            )

            /*
             * Find the staff account using the string login user ID.
             */
            val staffAccountRow =
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
                                normalizedStaffUserId
                    }
                    .limit(1)
                    .singleOrNull()
                    ?: throw IllegalArgumentException(
                        "No staff account was found with user ID " +
                                "'$normalizedStaffUserId'."
                    )

            val staffAccountId =
                staffAccountRow[
                    AccountTable.id
                ].value

            val staffName =
                staffAccountRow[
                    AccountTable.fullName
                ]

            val staffRole =
                staffAccountRow[
                    AccountTable.role
                ]
                    .trim()
                    .lowercase()

            val staffAccountIsActive =
                staffAccountRow[
                    AccountTable.isActive
                ]

            require(staffAccountIsActive) {
                "The staff account is inactive."
            }

            require(
                staffRole == "staff" ||
                        staffRole == "teacher"
            ) {
                "The supplied user ID does not belong to a staff member."
            }

            /*
             * The user ID in the URL must belong to the
             * authenticated staff account.
             */
            require(
                authenticatedAccountId ==
                        staffAccountId
            ) {
                "You cannot access another staff member's assigned class."
            }

            /*
             * Find the staff profile.
             */
            val staffProfileRow =
                StaffTable
                    .select(
                        StaffTable.id,
                        StaffTable.user,
                        StaffTable.assignedClass
                    )
                    .where {
                        StaffTable.user eq
                                staffAccountId
                    }
                    .limit(1)
                    .singleOrNull()
                    ?: throw IllegalArgumentException(
                        "The staff profile was not found."
                    )

            val staffProfileId =
                staffProfileRow[
                    StaffTable.id
                ].value

            val assignedClassId =
                staffProfileRow[
                    StaffTable.assignedClass
                ]?.value
                    ?: throw IllegalArgumentException(
                        "$staffName has not been assigned to a class."
                    )

            /*
             * Find the teacher's assigned class.
             */
            val assignedClassRow =
                NewGradeClassTable
                    .select(
                        NewGradeClassTable.id,
                        NewGradeClassTable.name,
                        NewGradeClassTable.isActive
                    )
                    .where {
                        NewGradeClassTable.id eq
                                assignedClassId
                    }
                    .limit(1)
                    .singleOrNull()
                    ?: throw IllegalArgumentException(
                        "The assigned class was not found."
                    )

            val assignedClassName =
                assignedClassRow[
                    NewGradeClassTable.name
                ]

            require(
                assignedClassRow[
                    NewGradeClassTable.isActive
                ]
            ) {
                "The assigned class is inactive."
            }

            /*
             * Join student profiles with their accounts.
             *
             * The join works through:
             *
             * StudentsTable.user
             *     -> AccountTable.id
             */
            val students =
                StudentsTable
                    .innerJoin(
                        AccountTable
                    )
                    .select(
                        StudentsTable.id,
                        StudentsTable.user,
                        StudentsTable.currentNewGradeClass,
                        StudentsTable.isGraduated,
                        AccountTable.id,
                        AccountTable.userId,
                        AccountTable.fullName,
                        AccountTable.role,
                        AccountTable.isActive
                    )
                    .where {
                        (
                                StudentsTable.currentNewGradeClass eq
                                        assignedClassId
                                ) and
                                (
                                        StudentsTable.isGraduated eq
                                                false
                                        ) and
                                (
                                        AccountTable.isActive eq
                                                true
                                        )
                    }
                    .orderBy(
                        AccountTable.fullName,
                        SortOrder.ASC
                    )
                    .mapNotNull { row ->

                        val accountRole =
                            row[
                                AccountTable.role
                            ]
                                .trim()
                                .lowercase()

                        /*
                         * Ignore any non-student account that was
                         * incorrectly linked to a student profile.
                         */
                        if (accountRole != "student") {
                            return@mapNotNull null
                        }

                        StaffClassStudentResponse(
                            studentId =
                                row[
                                    StudentsTable.id
                                ].value,

                            accountId =
                                row[
                                    AccountTable.id
                                ].value,

                            userId =
                                row[
                                    AccountTable.userId
                                ],

                            fullName =
                                row[
                                    AccountTable.fullName
                                ],

                            isActive =
                                row[
                                    AccountTable.isActive
                                ]
                        )
                    }

            StaffAssignedClassResponse(
                staffId =
                    staffProfileId,

                staffAccountId =
                    staffAccountId,

                staffUserId =
                    normalizedStaffUserId,

                staffName =
                    staffName,

                classId =
                    assignedClassId,

                className =
                    assignedClassName,

                studentCount =
                    students.size,

                students =
                    students
            )
        }
    }
}