package com.example.student.repos

import com.example.account.AccountTable
import com.example.minimals.FamilyMinimal
import com.example.staff.dtos.response.StudentLiteResponse
import com.example.student.StudentsTable
import com.example.student.dtos.requests.PatchStudentRequest
import com.example.familyfees.tables.FamilyTable
import org.jetbrains.exposed.sql.leftJoin
import com.example.student.dtos.requests.UpdateStudentRequest
import com.example.student.dtos.response.GradeClassResponse
import com.example.student.dtos.response.PerClassResponse
import com.example.student.dtos.response.StudentProfileResponse
import com.example.student.dtos.response.StudentUserResponse
import com.example.student.mappers.toStudentProfile
import com.example.student.models.StudentProfile
import com.example.student.tables.NewGradeClassTable
import org.jetbrains.exposed.sql.or
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
import org.jetbrains.exposed.sql.count

object StudentRepository {

    /**
     * Use this when you are already inside the correct tenant transaction/schema.
     */
    fun createInCurrentTransaction(profile: StudentProfile): StudentProfileResponse {
        val id = StudentsTable.insertAndGetId {
            it[user] = EntityID(profile.user, AccountTable)

            it[currentNewGradeClass] = profile.currentNewGradeClassId?.let { classId ->
                EntityID(classId, NewGradeClassTable)
            }

            it[family] = profile.family?.let { familyId ->
                EntityID(familyId, FamilyTable)
            }

            it[isGraduated] = profile.isGraduated
            it[lastSchoolAttended] = profile.lastSchoolAttended
            it[isDiscountedStudent] = profile.isDiscountedStudent
            it[isImmunized] = profile.isImmunized
            it[hasAllergies] = profile.hasAllergies
            it[allergicFoods] = profile.allergicFoods
            it[otherRelatedInfo] = profile.otherRelatedInfo
            it[nameOfFather] = profile.nameOfFather
            it[nameOfMother] = profile.nameOfMother
            it[occupationOfFather] = profile.occupationOfFather
            it[occupationOfMother] = profile.occupationOfMother
            it[nationalityOfFather] = profile.nationalityOfFather
            it[nationalityOfMother] = profile.nationalityOfMother
            it[contactOfFather] = profile.contactOfFather
            it[contactOfMother] = profile.contactOfMother
            it[houseNumber] = profile.houseNumber
        }.value

        return findByIdWithUserAndClassInCurrentTransaction(id)
            ?: throw IllegalStateException("Student profile was created but could not be retrieved.")
    }

    /**
     * Wrapper version if you want repository to open a transaction itself.
     */
    fun create(
        tenantSchema: String,
        profile: StudentProfile
    ): StudentProfileResponse = transaction {

        setTenantSchema(tenantSchema)

        createInCurrentTransaction(profile)
    }

    /**
     * Use this when you are already inside the correct tenant transaction/schema.
     */
    fun findAllInCurrentTransaction(): List<StudentProfile> {
        return StudentsTable
            .selectAll()
            .orderBy(StudentsTable.id, SortOrder.DESC)
            .map { it.toStudentProfile() }
    }

    /**
     * Wrapper version if you want repository to open a transaction itself.
     */
    fun findAll(tenantSchema: String): List<StudentProfile> = transaction {

        setTenantSchema(tenantSchema)

        findAllInCurrentTransaction()
    }

    /**
     * Use this when you are already inside the correct tenant transaction/schema.
     */
    fun findByIdInCurrentTransaction(id: Int): StudentProfile? {
        return StudentsTable
            .selectAll()
            .where { StudentsTable.id eq id }
            .singleOrNull()
            ?.toStudentProfile()
    }

    /**
     * Wrapper version if you want repository to open a transaction itself.
     */
    fun findById(
        tenantSchema: String,
        id: Int
    ): StudentProfile? = transaction {

        setTenantSchema(tenantSchema)

        findByIdInCurrentTransaction(id)
    }

    /**
     * Use this when you are already inside the correct tenant transaction/schema.
     */
    fun findByUserIdInCurrentTransaction(userId: Int): StudentProfile? {
        return StudentsTable
            .selectAll()
            .where { StudentsTable.user eq EntityID(userId, AccountTable) }
            .singleOrNull()
            ?.toStudentProfile()
    }

    /**
     * Wrapper version if you want repository to open a transaction itself.
     */
    fun findByUserId(
        tenantSchema: String,
        userId: Int
    ): StudentProfile? = transaction {

        setTenantSchema(tenantSchema)

        findByUserIdInCurrentTransaction(userId)
    }


    /**
     * Use this when you are already inside the correct tenant transaction/schema.
     */
    fun updateInCurrentTransaction(id: Int, profile: StudentProfile): Boolean {
        return StudentsTable.update({ StudentsTable.id eq id }) {
            it[currentNewGradeClass] = profile.currentNewGradeClassId?.let { classId ->
                EntityID(classId, NewGradeClassTable)
            }

            it[family] = profile.family?.let { familyId ->
                EntityID(familyId, FamilyTable)
            }

            it[isGraduated] = profile.isGraduated
            it[lastSchoolAttended] = profile.lastSchoolAttended
            it[isDiscountedStudent] = profile.isDiscountedStudent
            it[isImmunized] = profile.isImmunized
            it[hasAllergies] = profile.hasAllergies
            it[allergicFoods] = profile.allergicFoods
            it[otherRelatedInfo] = profile.otherRelatedInfo
            it[nameOfFather] = profile.nameOfFather
            it[nameOfMother] = profile.nameOfMother
            it[occupationOfFather] = profile.occupationOfFather
            it[occupationOfMother] = profile.occupationOfMother
            it[nationalityOfFather] = profile.nationalityOfFather
            it[nationalityOfMother] = profile.nationalityOfMother
            it[contactOfFather] = profile.contactOfFather
            it[contactOfMother] = profile.contactOfMother
            it[houseNumber] = profile.houseNumber
        } > 0
    }

    /**
     * Wrapper version if you want repository to open a transaction itself.
     */
    fun update(
        tenantSchema: String,
        id: Int,
        profile: StudentProfile
    ): Boolean = transaction {

        setTenantSchema(tenantSchema)

        updateInCurrentTransaction(id, profile)
    }


    /**
     * Use this when you are already inside the correct tenant transaction/schema.
     */
    fun findAllWithUserAndClassRawInCurrentTransaction(
        search: String?
    ): List<StudentProfileResponse> {
        val query = StudentsTable
            .join(AccountTable, JoinType.INNER, StudentsTable.user, AccountTable.id)
            .join(NewGradeClassTable, JoinType.LEFT, StudentsTable.currentNewGradeClass, NewGradeClassTable.id)
            .join(FamilyTable, JoinType.LEFT, StudentsTable.family, FamilyTable.id)
            .selectAll()

        if (!search.isNullOrBlank()) {
            val q = "%${search.lowercase()}%"
            query.andWhere {
                (AccountTable.fullName.lowerCase() like q) or
                        (NewGradeClassTable.name.lowerCase() like q) or
                        (StudentsTable.contactOfFather.lowerCase() like q) or
                        (StudentsTable.contactOfMother.lowerCase() like q)
            }
        }

        return query
            .orderBy(StudentsTable.id, SortOrder.DESC)
            .map { row ->
                val user = StudentUserResponse(
                    id = row[AccountTable.id].value,
                    userId = row[AccountTable.userId],
                    fullName = row[AccountTable.fullName],
                    gender = row[AccountTable.gender],
                    role = row[AccountTable.role],
                    isActive = row[AccountTable.isActive],
                    pin = row[AccountTable.pin],
                    dateOfBirth = row[AccountTable.dateOfBirth],
                    profilePictureUrl = row[AccountTable.profilePictureUrl],
                    profilePicturePublicId = row[AccountTable.profilePicturePublicId]
                )

                val gradeClass = row[NewGradeClassTable.id]?.value?.let {
                    GradeClassResponse(it, row[NewGradeClassTable.name])
                }

                val family = row[FamilyTable.id]?.value?.let {
                    FamilyMinimal(it, row[FamilyTable.name])
                }

                StudentProfileResponse(
                    id = row[StudentsTable.id].value,
                    user = user,
                    currentNewGradeClass = gradeClass,
                    family = family,
                    isGraduated = row[StudentsTable.isGraduated],
                    lastSchoolAttended = row[StudentsTable.lastSchoolAttended],
                    isDiscountedStudent = row[StudentsTable.isDiscountedStudent],
                    isImmunized = row[StudentsTable.isImmunized],
                    allergicFoods = row[StudentsTable.allergicFoods],
                    otherRelatedInfo = row[StudentsTable.otherRelatedInfo],
                    nameOfFather = row[StudentsTable.nameOfFather],
                    nameOfMother = row[StudentsTable.nameOfMother],
                    occupationOfFather = row[StudentsTable.occupationOfFather],
                    occupationOfMother = row[StudentsTable.occupationOfMother],
                    nationalityOfFather = row[StudentsTable.nationalityOfFather],
                    nationalityOfMother = row[StudentsTable.nationalityOfMother],
                    contactOfFather = row[StudentsTable.contactOfFather],
                    contactOfMother = row[StudentsTable.contactOfMother],
                    houseNumber = row[StudentsTable.houseNumber],
                    hasAllergies = row[StudentsTable.hasAllergies]
                )
            }
    }

    /**
     * Wrapper version if you want repository to open a transaction itself.
     */
    fun findAllWithUserAndClassRaw(
        tenantSchema: String,
        search: String?
    ): List<StudentProfileResponse> = transaction {

        setTenantSchema(tenantSchema)

        findAllWithUserAndClassRawInCurrentTransaction(search)
    }


    /**
     * Use this when you are already inside the correct tenant transaction/schema.
     */
    fun countStudentsInCurrentTransaction(): Int {
        return StudentsTable.selectAll().count().toInt()
    }

    /**
     * Wrapper version if you want repository to open a transaction itself.
     */
    fun countStudents(
        tenantSchema: String
    ): Int = transaction {

        setTenantSchema(tenantSchema)

        countStudentsInCurrentTransaction()
    }
    /**
     * Use this when you are already inside the correct tenant transaction/schema.
     */
    fun findAllWithUserAndClassInCurrentTransaction(
        page: Int,
        limit: Int,
        search: String?
    ): Pair<List<StudentProfileResponse>, Long> {
        val offset = ((page - 1) * limit).toLong()

        val query = StudentsTable
            .join(AccountTable, JoinType.INNER, StudentsTable.user, AccountTable.id)
            .join(NewGradeClassTable, JoinType.LEFT, StudentsTable.currentNewGradeClass, NewGradeClassTable.id)
            .join(FamilyTable, JoinType.LEFT, StudentsTable.family, FamilyTable.id)
            .selectAll()

        if (!search.isNullOrBlank()) {
            val q = "%${search.lowercase()}%"
            query.andWhere {
                (AccountTable.fullName.lowerCase() like q) or
                        (NewGradeClassTable.name.lowerCase() like q) or
                        (StudentsTable.contactOfFather.lowerCase() like q) or
                        (StudentsTable.contactOfMother.lowerCase() like q)
            }
        }

        val total = query.count()

        val data = query
            .orderBy(StudentsTable.id, SortOrder.DESC)
            .limit(limit)
            .offset(offset)
            .map { row ->
                val user = StudentUserResponse(
                    id = row[AccountTable.id].value,
                    userId = row[AccountTable.userId],
                    fullName = row[AccountTable.fullName],
                    gender = row[AccountTable.gender],
                    role = row[AccountTable.role],
                    isActive = row[AccountTable.isActive],
                    pin = row[AccountTable.pin],
                    dateOfBirth = row[AccountTable.dateOfBirth],
                    profilePictureUrl = row[AccountTable.profilePictureUrl],
                    profilePicturePublicId = row[AccountTable.profilePicturePublicId]
                )

                val gradeClass = row[NewGradeClassTable.id]?.value?.let {
                    GradeClassResponse(it, row[NewGradeClassTable.name])
                }

                val family = row[FamilyTable.id]?.value?.let {
                    FamilyMinimal(it, row[FamilyTable.name])
                }

                StudentProfileResponse(
                    id = row[StudentsTable.id].value,
                    user = user,
                    currentNewGradeClass = gradeClass,
                    family = family,
                    isGraduated = row[StudentsTable.isGraduated],
                    lastSchoolAttended = row[StudentsTable.lastSchoolAttended],
                    isDiscountedStudent = row[StudentsTable.isDiscountedStudent],
                    isImmunized = row[StudentsTable.isImmunized],
                    allergicFoods = row[StudentsTable.allergicFoods],
                    otherRelatedInfo = row[StudentsTable.otherRelatedInfo],
                    nameOfFather = row[StudentsTable.nameOfFather],
                    nameOfMother = row[StudentsTable.nameOfMother],
                    occupationOfFather = row[StudentsTable.occupationOfFather],
                    occupationOfMother = row[StudentsTable.occupationOfMother],
                    nationalityOfFather = row[StudentsTable.nationalityOfFather],
                    nationalityOfMother = row[StudentsTable.nationalityOfMother],
                    contactOfFather = row[StudentsTable.contactOfFather],
                    contactOfMother = row[StudentsTable.contactOfMother],
                    houseNumber = row[StudentsTable.houseNumber],
                    hasAllergies = row[StudentsTable.hasAllergies]
                )
            }

        return Pair(data, total)
    }

    /**
     * Wrapper version if you want repository to open a transaction itself.
     */
    fun findAllWithUserAndClass(
        tenantSchema: String,
        page: Int,
        limit: Int,
        search: String?
    ): Pair<List<StudentProfileResponse>, Long> = transaction {

        setTenantSchema(tenantSchema)

        findAllWithUserAndClassInCurrentTransaction(page, limit, search)
    }


    /**
     * Use this when you are already inside the correct tenant transaction/schema.
     */
    fun existsByIdInCurrentTransaction(id: Int): Boolean {
        return StudentsTable
            .selectAll()
            .where { StudentsTable.id eq id }
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
    fun updateFullInCurrentTransaction(
        id: Int,
        req: UpdateStudentRequest
    ): Boolean {
        return StudentsTable.update({ StudentsTable.id eq id }) { row ->
            row[StudentsTable.currentNewGradeClass] =
                req.currentNewGradeClassId?.let { EntityID(it, NewGradeClassTable) }

            row[StudentsTable.isGraduated] = req.isGraduated
            row[StudentsTable.lastSchoolAttended] = req.lastSchoolAttended
            row[StudentsTable.isDiscountedStudent] = req.isDiscountedStudent
            row[StudentsTable.isImmunized] = req.isImmunized
            row[StudentsTable.hasAllergies] = req.hasAllergies
            row[StudentsTable.allergicFoods] = req.allergicFoods
            row[StudentsTable.otherRelatedInfo] = req.otherRelatedInfo
            row[StudentsTable.nameOfFather] = req.nameOfFather
            row[StudentsTable.nameOfMother] = req.nameOfMother
            row[StudentsTable.occupationOfFather] = req.occupationOfFather
            row[StudentsTable.occupationOfMother] = req.occupationOfMother
            row[StudentsTable.nationalityOfFather] = req.nationalityOfFather
            row[StudentsTable.nationalityOfMother] = req.nationalityOfMother
            row[StudentsTable.contactOfFather] = req.contactOfFather
            row[StudentsTable.contactOfMother] = req.contactOfMother
            row[StudentsTable.houseNumber] = req.houseNumber
        } > 0
    }

    /**
     * Wrapper version if you want repository to open a transaction itself.
     */
    fun updateFull(id: Int, req: UpdateStudentRequest): Boolean = transaction {
        updateFullInCurrentTransaction(id, req)
    }

    /**
     * Use this when you are already inside the correct tenant transaction/schema.
     */
    fun patchInCurrentTransaction(
        id: Int,
        req: PatchStudentRequest
    ): Boolean {
        val existing = StudentsTable
            .selectAll()
            .where { StudentsTable.id eq id }
            .singleOrNull()
            ?: return false

        val accountId = existing[StudentsTable.user].value

        val updated = StudentsTable.update({ StudentsTable.id eq id }) { row ->
            if (req.currentNewGradeClassId != null) {
                row[StudentsTable.currentNewGradeClass] =
                    EntityID(req.currentNewGradeClassId, NewGradeClassTable)
            } else {
                row[StudentsTable.currentNewGradeClass] = null
            }

            req.lastSchoolAttended?.let { row[StudentsTable.lastSchoolAttended] = it }
            req.isDiscountedStudent?.let { row[StudentsTable.isDiscountedStudent] = it }
            req.isImmunized?.let { row[StudentsTable.isImmunized] = it }
            req.allergicFoods?.let { row[StudentsTable.allergicFoods] = it }
            req.otherRelatedInfo?.let { row[StudentsTable.otherRelatedInfo] = it }
            req.nameOfFather?.let { row[StudentsTable.nameOfFather] = it }
            req.occupationOfFather?.let { row[StudentsTable.occupationOfFather] = it }
            req.nationalityOfFather?.let { row[StudentsTable.nationalityOfFather] = it }
            req.nameOfMother?.let { row[StudentsTable.nameOfMother] = it }
            req.occupationOfMother?.let { row[StudentsTable.occupationOfMother] = it }
            req.nationalityOfMother?.let { row[StudentsTable.nationalityOfMother] = it }
            req.contactOfFather?.let { row[StudentsTable.contactOfFather] = it }
            req.contactOfMother?.let { row[StudentsTable.contactOfMother] = it }
            req.houseNumber?.let { row[StudentsTable.houseNumber] = it }
        }

        if (updated == 0) {
            return false
        }

        req.user?.let { userPatch ->
            AccountTable.update({ AccountTable.id eq accountId }) { a ->
                userPatch.fullName?.let { a[AccountTable.fullName] = it }
                userPatch.gender?.let { a[AccountTable.gender] = it }
                userPatch.nationality?.let { a[AccountTable.nationality] = it }
                userPatch.dateOfBirth?.let { a[AccountTable.dateOfBirth] = it }
                userPatch.role?.let { a[AccountTable.role] = it.lowercase() }
                userPatch.isActive?.let { a[AccountTable.isActive] = it }
                userPatch.isStaff?.let { a[AccountTable.isStaff] = it }
            }
        }

        return true
    }

    /**
     * Wrapper version if you want repository to open a transaction itself.
     */
    fun patch(
        tenantSchema: String,
        id: Int,
        req: PatchStudentRequest
    ): Boolean = transaction {

        setTenantSchema(tenantSchema)

        patchInCurrentTransaction(id, req)
    }


    /**
     * Use this when you are already inside the correct tenant transaction/schema.
     */
    fun deleteInCurrentTransaction(id: Int): Boolean {
        return StudentsTable.deleteWhere { StudentsTable.id eq id } > 0
    }

    /**
     * Wrapper version if you want repository to open a transaction itself.
     */
    fun delete(
        tenantSchema: String,
        id: Int
    ): Boolean = transaction {

        setTenantSchema(tenantSchema)

        deleteInCurrentTransaction(id)
    }

    /**
     * Use this when you are already inside the correct tenant transaction/schema.
     */
    fun patchNestedInCurrentTransaction(
        studentProfileId: Int,
        req: PatchStudentRequest
    ): StudentProfileResponse? {
        val row = StudentsTable
            .selectAll()
            .where { StudentsTable.id eq studentProfileId }
            .singleOrNull()
            ?: return null

        val accountId = row[StudentsTable.user].value

        StudentsTable.update({ StudentsTable.id eq studentProfileId }) { u ->
            if (req.currentNewGradeClassId != null) {
                u[StudentsTable.currentNewGradeClass] =
                    EntityID(req.currentNewGradeClassId, NewGradeClassTable)
            }

            u[StudentsTable.family] =
                req.family?.let { EntityID(it, FamilyTable) }

            req.lastSchoolAttended?.let { u[StudentsTable.lastSchoolAttended] = it }
            req.isDiscountedStudent?.let { u[StudentsTable.isDiscountedStudent] = it }
            req.isImmunized?.let { u[StudentsTable.isImmunized] = it }
            req.allergicFoods?.let { u[StudentsTable.allergicFoods] = it }
            req.otherRelatedInfo?.let { u[StudentsTable.otherRelatedInfo] = it }
            req.nameOfFather?.let { u[StudentsTable.nameOfFather] = it }
            req.nameOfMother?.let { u[StudentsTable.nameOfMother] = it }
            req.occupationOfFather?.let { u[StudentsTable.occupationOfFather] = it }
            req.occupationOfMother?.let { u[StudentsTable.occupationOfMother] = it }
            req.nationalityOfFather?.let { u[StudentsTable.nationalityOfFather] = it }
            req.nationalityOfMother?.let { u[StudentsTable.nationalityOfMother] = it }
            req.contactOfFather?.let { u[StudentsTable.contactOfFather] = it }
            req.contactOfMother?.let { u[StudentsTable.contactOfMother] = it }
            req.houseNumber?.let { u[StudentsTable.houseNumber] = it }
        }

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

        return findByIdWithUserAndClassInCurrentTransaction(studentProfileId)
    }

    /**
     * Wrapper version if you want repository to open a transaction itself.
     */
    fun patchNested(
        tenantSchema: String,
        studentProfileId: Int,
        req: PatchStudentRequest
    ): StudentProfileResponse? = transaction {

        setTenantSchema(tenantSchema)

        patchNestedInCurrentTransaction(studentProfileId, req)
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
            .map { row ->
                StudentLiteResponse(
                    id = row[StudentsTable.id].value,
                    full_name = row[AccountTable.fullName],
                    indexNo = row[AccountTable.userId]
                )
            }
    }

    /**
     * Wrapper version if you want repository to open a transaction itself.
     */
    fun findStudentsByClass(
        tenantSchema: String,
        classId: Int
    ): List<StudentLiteResponse> = transaction {

        setTenantSchema(tenantSchema)

        findStudentsByClassInCurrentTransaction(classId)
    }

    /**
     * Use this when you are already inside the correct tenant transaction/schema.
     */
    fun findByIdWithUserAndClassInCurrentTransaction(
        studentProfileId: Int
    ): StudentProfileResponse? {
        val query = StudentsTable
            .join(
                otherTable = AccountTable,
                joinType = JoinType.INNER,
                onColumn = StudentsTable.user,
                otherColumn = AccountTable.id
            )
            .join(
                otherTable = NewGradeClassTable,
                joinType = JoinType.LEFT,
                onColumn = StudentsTable.currentNewGradeClass,
                otherColumn = NewGradeClassTable.id
            )
            .join(
                otherTable = FamilyTable,
                joinType = JoinType.LEFT,
                onColumn = StudentsTable.family,
                otherColumn = FamilyTable.id
            )

        return query
            .selectAll()
            .where { StudentsTable.id eq studentProfileId }
            .singleOrNull()
            ?.let { r ->
                val userDto = StudentUserResponse(
                    id = r[AccountTable.id].value,
                    userId = r[AccountTable.userId],
                    fullName = r[AccountTable.fullName],
                    gender = r[AccountTable.gender],
                    role = r[AccountTable.role],
                    isActive = r[AccountTable.isActive],
                    pin = r[AccountTable.pin],
                    dateOfBirth = r[AccountTable.dateOfBirth],
                    profilePictureUrl = r[AccountTable.profilePictureUrl],
                    profilePicturePublicId = r[AccountTable.profilePicturePublicId]
                )

                val classDto = r[NewGradeClassTable.id]?.value?.let { classId ->
                    GradeClassResponse(
                        id = classId,
                        name = r[NewGradeClassTable.name]
                    )
                }

                val familyDto = r[FamilyTable.id]?.value?.let { familyId ->
                    FamilyMinimal(
                        id = familyId,
                        name = r[FamilyTable.name]
                    )
                }

                StudentProfileResponse(
                    id = r[StudentsTable.id].value,
                    user = userDto,
                    currentNewGradeClass = classDto,
                    family = familyDto,
                    isGraduated = r[StudentsTable.isGraduated],
                    lastSchoolAttended = r[StudentsTable.lastSchoolAttended],
                    isDiscountedStudent = r[StudentsTable.isDiscountedStudent],
                    isImmunized = r[StudentsTable.isImmunized],
                    hasAllergies = r[StudentsTable.hasAllergies],
                    allergicFoods = r[StudentsTable.allergicFoods],
                    otherRelatedInfo = r[StudentsTable.otherRelatedInfo],
                    nameOfFather = r[StudentsTable.nameOfFather],
                    nameOfMother = r[StudentsTable.nameOfMother],
                    occupationOfFather = r[StudentsTable.occupationOfFather],
                    occupationOfMother = r[StudentsTable.occupationOfMother],
                    nationalityOfFather = r[StudentsTable.nationalityOfFather],
                    nationalityOfMother = r[StudentsTable.nationalityOfMother],
                    contactOfFather = r[StudentsTable.contactOfFather],
                    contactOfMother = r[StudentsTable.contactOfMother],
                    houseNumber = r[StudentsTable.houseNumber]
                )
            }
    }

    /**
     * Wrapper version if you want repository to open a transaction itself.
     */
    fun findByIdWithUserAndClass(studentProfileId: Int): StudentProfileResponse? = transaction {
        findByIdWithUserAndClassInCurrentTransaction(studentProfileId)
    }

    /**
     * Use this when you are already inside the correct tenant transaction/schema.
     */
    fun countPerClassInCurrentTransaction(): List<PerClassResponse> {
        val countExpr = StudentsTable.id.count()

        return StudentsTable
            .leftJoin(
                NewGradeClassTable,
                { StudentsTable.currentNewGradeClass },
                { NewGradeClassTable.id }
            )
            .select(NewGradeClassTable.name, countExpr)
            .groupBy(NewGradeClassTable.name)
            .orderBy(NewGradeClassTable.name to SortOrder.ASC)
            .map { row ->
                PerClassResponse(
                    `class` = row[NewGradeClassTable.name] ?: "No Class Assigned",
                    count = row[countExpr]
                )
            }
    }

    /**
     * Wrapper version if you want repository to open a transaction itself.
     */
    fun countPerClass(
        tenantSchema: String
    ): List<PerClassResponse> = transaction {

        setTenantSchema(tenantSchema)

        countPerClassInCurrentTransaction()
    }
}
