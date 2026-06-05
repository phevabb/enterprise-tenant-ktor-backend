package com.example.staff.repos

import com.example.academics.repos.setTenantSchema
import com.example.account.AccountTable
import com.example.staff.dtos.requests.PatchStaffRequest

import com.example.student.StudentsTable
import com.example.student.dtos.response.GradeClassResponse
import com.example.student.dtos.response.StudentUserResponse
import com.example.student.tables.NewGradeClassTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like

import com.example.staff.dtos.response.StaffProfileResponse
import com.example.staff.dtos.response.StudentLiteResponse
import com.example.staff.mappers.toStaffProfile
import com.example.staff.models.StaffProfile
import com.example.staff.tables.StaffTable
import org.jetbrains.exposed.sql.or

object StaffRepository {

    /**
     * Use this when you are already inside the correct tenant transaction/schema.
     */
    fun createInCurrentTransaction(profile: StaffProfile): StaffProfileResponse {
        val id = StaffTable.insertAndGetId {
            it[user] = profile.user?.let { userId ->
                EntityID(userId, AccountTable)
            }

            it[assignedClass] = profile.assignedClassId?.let { classId ->
                EntityID(classId, NewGradeClassTable)
            }

            it[tel] = profile.tel
        }.value

        return findByIdWithUserAndClassInCurrentTransaction(id)
            ?: throw IllegalStateException("Staff profile was created but could not be retrieved.")
    }

    /**
     * Wrapper version if you want repository to open a transaction itself.
     */
    fun create(profile: StaffProfile): StaffProfileResponse = transaction {
        createInCurrentTransaction(profile)
    }

    /**
     * Use this when you are already inside the correct tenant transaction/schema.
     */
    fun findAllInCurrentTransaction(): List<StaffProfile> {
        return StaffTable
            .selectAll()
            .orderBy(StaffTable.id, SortOrder.DESC)
            .map { it.toStaffProfile() }
    }

    /**
     * Wrapper version if you want repository to open a transaction itself.
     */
    fun findAll(): List<StaffProfile> = transaction {
        findAllInCurrentTransaction()
    }

    /**
     * Use this when you are already inside the correct tenant transaction/schema.
     */
    fun findByIdInCurrentTransaction(id: Int): StaffProfile? {
        return StaffTable
            .selectAll()
            .where { StaffTable.id eq id }
            .singleOrNull()
            ?.toStaffProfile()
    }

    /**
     * Wrapper version if you want repository to open a transaction itself.
     */
    fun findById(id: Int): StaffProfile? = transaction {
        findByIdInCurrentTransaction(id)
    }

    /**
     * Use this when you are already inside the correct tenant transaction/schema.
     */
    fun findByUserIdInCurrentTransaction(userId: Int): StaffProfile? {
        return StaffTable
            .selectAll()
            .where { StaffTable.user eq EntityID(userId, AccountTable) }
            .singleOrNull()
            ?.toStaffProfile()
    }

    /**
     * Wrapper version if you want repository to open a transaction itself.
     */
    fun findByUserId(
        tenantSchema: String,
        userId: Int
    ): StaffProfile? = transaction {

        setTenantSchema(tenantSchema)
        findByUserIdInCurrentTransaction(userId)
    }

    /**
     * Use this when you are already inside the correct tenant transaction/schema.
     */
    fun deleteInCurrentTransaction(id: Int): Boolean {
        return StaffTable.deleteWhere { StaffTable.id eq id } > 0
    }

    /**
     * Wrapper version if you want repository to open a transaction itself.
     */
    fun delete(id: Int): Boolean = transaction {
        deleteInCurrentTransaction(id)
    }

    /**
     * Use this when you are already inside the correct tenant transaction/schema.
     */
    fun existsByIdInCurrentTransaction(id: Int): Boolean {
        return StaffTable
            .selectAll()
            .where { StaffTable.id eq id }
            .count() > 0
    }

    /**
     * Wrapper version if you want repository to open a transaction itself.
     */
    fun existsById(id: Int): Boolean = transaction {
        existsByIdInCurrentTransaction(id)
    }

    /**
     * Use this when you are already inside the correct tenant transaction/schema.
     */
    fun findAllWithUserAndClassInCurrentTransaction(
        search: String?
    ): List<StaffProfileResponse> {
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

        return query.map { row ->
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

    /**
     * Wrapper version if you want repository to open a transaction itself.
     */
    fun findAllWithUserAndClass(
        tenantSchema: String,
        search: String?
    ): List<StaffProfileResponse> = transaction {

        require(tenantSchema.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            "Invalid tenant schema"
        }

        setTenantSchema(tenantSchema)
        findAllWithUserAndClassInCurrentTransaction(search)
    }


    /**
     * Use this when you are already inside the correct tenant transaction/schema.
     */
    fun findByIdWithUserAndClassInCurrentTransaction(id: Int): StaffProfileResponse? {
        val query = StaffTable
            .join(AccountTable, JoinType.INNER, StaffTable.user, AccountTable.id)
            .join(NewGradeClassTable, JoinType.LEFT, StaffTable.assignedClass, NewGradeClassTable.id)

        return query
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

    /**
     * Wrapper version if you want repository to open a transaction itself.
     */
    fun findByIdWithUserAndClass(id: Int): StaffProfileResponse? = transaction {
        findByIdWithUserAndClassInCurrentTransaction(id)
    }

    /**
     * Use this when you are already inside the correct tenant transaction/schema.
     */
    fun patchNestedInCurrentTransaction(
        id: Int,
        req: PatchStaffRequest
    ): StaffProfileResponse? {
        val row = StaffTable
            .selectAll()
            .where { StaffTable.id eq id }
            .singleOrNull()
            ?: return null

        val accountId = row[StaffTable.user]?.value

        StaffTable.update({ StaffTable.id eq id }) { u ->
            if (req.assignedClassId != null) {
                u[StaffTable.assignedClass] = EntityID(req.assignedClassId, NewGradeClassTable)
            } else {
                u[StaffTable.assignedClass] = null
            }

            req.tel?.let { u[StaffTable.tel] = it }
        }

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

        return findByIdWithUserAndClassInCurrentTransaction(id)
    }

    /**
     * Wrapper version if you want repository to open a transaction itself.
     */
    fun patchNested(
        tenantSchema: String,
        id: Int,
        req: PatchStaffRequest
    ): StaffProfileResponse? = transaction {

        setTenantSchema(tenantSchema)
        patchNestedInCurrentTransaction(id, req)
    }

    /**
     * Use this when you are already inside the correct tenant transaction/schema.
     */
    fun findStudentsByClassInCurrentTransaction(classId: Int): List<StudentLiteResponse> {
        return StudentsTable
            .join(AccountTable, JoinType.INNER, StudentsTable.user, AccountTable.id)
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

    /**
     * Wrapper version if you want repository to open a transaction itself.
     */
    fun findStudentsByClass(classId: Int): List<StudentLiteResponse> = transaction {
        findStudentsByClassInCurrentTransaction(classId)
    }
}

