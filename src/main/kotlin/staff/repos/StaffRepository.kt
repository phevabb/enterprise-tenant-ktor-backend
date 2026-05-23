package com.example.staff.repos


import com.example.staff.models.StaffProfile
import com.example.staff.mappers.toStaffProfile
import com.example.staff.dtos.response.StaffProfileResponse

import com.example.account.AccountTable
import com.example.staff.dtos.requests.PatchStaffRequest
import com.example.staff.dtos.response.StudentLiteResponse
import com.example.staff.tables.StaffTable
import com.example.student.StudentsTable
import com.example.student.dtos.response.GradeClassResponse
import com.example.student.dtos.response.StudentUserResponse
import com.example.student.tables.NewGradeClassTable

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId

object StaffRepository {

    /** ✅ Create staff profile */
    fun create(profile: StaffProfile) = transaction {

        val id = StaffTable.insertAndGetId {

            it[user] = profile.user?.let {
                EntityID(it, AccountTable)
            }

            it[assignedClass] = profile.assignedClassId?.let {
                EntityID(it, NewGradeClassTable)
            }

            it[tel] = profile.tel

        }.value

        findByIdWithUserAndClass(id)!!
    }

    /** ✅ Get all staff */
    fun findAll(): List<StaffProfile> = transaction {
        StaffTable
            .selectAll()
            .orderBy(StaffTable.id, SortOrder.DESC)
            .map { it.toStaffProfile() }
    }

    /** ✅ Get by profile ID */
    fun findById(id: Int): StaffProfile? = transaction {
        StaffTable
            .selectAll()
            .where { StaffTable.id eq id }
            .singleOrNull()
            ?.toStaffProfile()
    }

    /** ✅ OneToOne lookup */
    fun findByUserId(userId: Int): StaffProfile? = transaction {
        StaffTable
            .selectAll()
            .where { StaffTable.user eq EntityID(userId, AccountTable) }
            .singleOrNull()
            ?.toStaffProfile()
    }

    /** ✅ Delete */
    fun delete(id: Int): Boolean = transaction {
        StaffTable.deleteWhere { StaffTable.id eq id } > 0
    }

    /** ✅ Exists */
    fun existsById(id: Int): Boolean = transaction {
        StaffTable
            .selectAll()
            .where { StaffTable.id eq id }
            .count() > 0
    }

    /**
     * ✅ JOIN → Return nested response (LIKE Django serializer)
     */
    fun findAllWithUserAndClass(search: String?): List<StaffProfileResponse> = transaction {

        val query = StaffTable
            .join(AccountTable, JoinType.INNER, StaffTable.user, AccountTable.id)
            .join(NewGradeClassTable, JoinType.LEFT, StaffTable.assignedClass, NewGradeClassTable.id)
            .selectAll()

        if (!search.isNullOrBlank()) {
            val q = "%${search.lowercase()}%"
            query.andWhere {
                (AccountTable.fullName.lowerCase() like q) or
                        (NewGradeClassTable.name.lowerCase() like q)
            }
        }

        query.map { row ->

            val user = StudentUserResponse(
                id = row[AccountTable.id].value,
                userId = row[AccountTable.userId],
                fullName = row[AccountTable.fullName],
                gender = row[AccountTable.gender],
                role = row[AccountTable.role],
                isActive = row[AccountTable.isActive],
                pin = row[AccountTable.pin],
                dateOfBirth = row[AccountTable.dateOfBirth]
            )

            val assignedClass = row[NewGradeClassTable.id]?.value?.let {
                GradeClassResponse(it, row[NewGradeClassTable.name])
            }

            StaffProfileResponse(
                id = row[StaffTable.id].value,
                user = user,
                assignedClass = assignedClass,
                tel = row[StaffTable.tel]
            )
        }
    }

    /** ✅ Single with JOIN (like Django detail view) */
    fun findByIdWithUserAndClass(id: Int): StaffProfileResponse? = transaction {

        val query = StaffTable
            .join(AccountTable, JoinType.INNER, StaffTable.user, AccountTable.id)
            .join(NewGradeClassTable, JoinType.LEFT, StaffTable.assignedClass, NewGradeClassTable.id)

        query
            .selectAll()
            .where { StaffTable.id eq id }
            .singleOrNull()
            ?.let { row ->

                val user = StudentUserResponse(
                    id = row[AccountTable.id].value,
                    userId = row[AccountTable.userId],
                    fullName = row[AccountTable.fullName],
                    gender = row[AccountTable.gender],
                    role = row[AccountTable.role],
                    isActive = row[AccountTable.isActive],
                    pin = row[AccountTable.pin],
                    dateOfBirth = row[AccountTable.dateOfBirth]
                )

                val assignedClass = row[NewGradeClassTable.id]?.value?.let {
                    GradeClassResponse(it, row[NewGradeClassTable.name])
                }

                StaffProfileResponse(
                    id = row[StaffTable.id].value,
                    user = user,
                    assignedClass = assignedClass,
                    tel = row[StaffTable.tel]
                )
            }
    }

    fun patchNested(id: Int, req: PatchStaffRequest): StaffProfileResponse? = transaction {

        val row = StaffTable
            .selectAll()
            .where { StaffTable.id eq id }
            .singleOrNull()
            ?: return@transaction null

        val accountId = row[StaffTable.user]?.value

        // ✅ update staff fields
        StaffTable.update({ StaffTable.id eq id }) { u ->

            if (req.assignedClassId != null) {
                u[StaffTable.assignedClass] =
                    EntityID(req.assignedClassId, NewGradeClassTable)
            } else {
                u[StaffTable.assignedClass] = null
            }

            req.tel?.let { u[StaffTable.tel] = it }
        }

        // ✅ update user (if present)
        if (accountId != null) {
            req.user?.let { userPatch ->
                AccountTable.update({ AccountTable.id eq accountId }) { a ->

                    userPatch.fullName?.let { a[AccountTable.fullName] = it }
                    userPatch.gender?.let { a[AccountTable.gender] = it }
                    userPatch.dateOfBirth?.let { a[AccountTable.dateOfBirth] = it }
                    userPatch.nationality?.let { a[AccountTable.nationality] = it }
                    userPatch.role?.let { a[AccountTable.role] = it.lowercase() }
                    userPatch.isActive?.let { a[AccountTable.isActive] = it }
                    userPatch.isStaff?.let { a[AccountTable.isStaff] = it }
                }
            }
        }

        // ✅ return updated data
        findByIdWithUserAndClass(id)
    }




        fun findStudentsByClass(classId: Int): List<StudentLiteResponse> = transaction {

            StudentsTable
                .join(
                    AccountTable,
                    JoinType.INNER,
                    StudentsTable.user,
                    AccountTable.id
                )
                .selectAll()
                .where { StudentsTable.currentNewGradeClass eq classId }
                .orderBy(AccountTable.fullName to SortOrder.ASC)
                .map {

                    StudentLiteResponse(
                        id = it[StudentsTable.id].value,
                        full_name = it[AccountTable.fullName],
                        indexNo = it[AccountTable.userId]
                    )
                }
        }

}