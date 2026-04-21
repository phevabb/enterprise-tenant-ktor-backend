package com.example.student.repos

import com.example.account.AccountTable
import com.example.student.StudentsTable
import com.example.student.dtos.requests.PatchStudentRequest
import com.example.student.dtos.requests.UpdateStudentRequest
import com.example.student.dtos.response.GradeClassResponse
import com.example.student.dtos.response.StudentProfileResponse
import com.example.student.dtos.response.StudentUserResponse
import com.example.student.models.StudentProfile
import com.example.student.mappers.toStudentProfile
import com.example.student.tables.NewGradeClassTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

object StudentRepository {

    /** ✅ Create student profile */
    fun create(profile: StudentProfile): StudentProfile = transaction {

        val id = StudentsTable.insertAndGetId {

            // ✅ FK → EntityID<Int>
            it[user] = EntityID(profile.user, AccountTable)

            // ✅ Nullable FK
            it[currentNewGradeClass] =
                profile.currentNewGradeClassId?.let {
                    EntityID(it, NewGradeClassTable)
                }

            it[isGraduated] = profile.isGraduated
            it[lastSchoolAttended] = profile.lastSchoolAttended

            it[isDiscountedStudent] = profile.isDiscountedStudent
            it[isImmunized] = profile.isImmunized
            it[hasAllergies] = profile.hasAllergies
            it[hasPeculiarHealthIssues] = profile.hasPeculiarHealthIssues

            it[allergicFoods] = profile.allergicFoods
            it[healthIssues] = profile.healthIssues
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

        findById(id)!!
    }

    /** ✅ Get all students */
    fun findAll(): List<StudentProfile> = transaction {
        StudentsTable
            .selectAll()
            .orderBy(StudentsTable.id, SortOrder.DESC)
            .map { it.toStudentProfile() }
    }

    /** ✅ Get student by profile ID */
    fun findById(id: Int): StudentProfile? = transaction {
        StudentsTable
            .selectAll()
            .where { StudentsTable.id eq id }
            .singleOrNull()
            ?.toStudentProfile()
    }

    /** ✅ Get student by account ID (OneToOne lookup) */
    fun findByUserId(userId: Int): StudentProfile? = transaction {
        StudentsTable
            .selectAll()
            .where { StudentsTable.user eq EntityID(userId, AccountTable) }
            .singleOrNull()
            ?.toStudentProfile()
    }

    /** ✅ Update student profile */
    fun update(id: Int, profile: StudentProfile): Boolean = transaction {
        StudentsTable.update({ StudentsTable.id eq id }) {

            it[currentNewGradeClass] =
                profile.currentNewGradeClassId?.let {
                    EntityID(it, NewGradeClassTable)
                }

            it[isGraduated] = profile.isGraduated
            it[lastSchoolAttended] = profile.lastSchoolAttended

            it[isDiscountedStudent] = profile.isDiscountedStudent
            it[isImmunized] = profile.isImmunized
            it[hasAllergies] = profile.hasAllergies
            it[hasPeculiarHealthIssues] = profile.hasPeculiarHealthIssues

            it[allergicFoods] = profile.allergicFoods
            it[healthIssues] = profile.healthIssues
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


    fun findAllWithUserAndClass(): List<StudentProfileResponse> = transaction {

            val query = StudentsTable
                .join(AccountTable, JoinType.INNER, onColumn = StudentsTable.user, otherColumn = AccountTable.id)
                .join(NewGradeClassTable, JoinType.LEFT, onColumn = StudentsTable.currentNewGradeClass, otherColumn = NewGradeClassTable.id)

            query
                .selectAll()
                .orderBy(StudentsTable.id, SortOrder.DESC)
                .map { row ->

                    val user = StudentUserResponse(
                        id = row[AccountTable.id].value,
                        userId = row[AccountTable.userId],
                        fullName = row[AccountTable.fullName],
                        gender = row[AccountTable.gender],
                        role = row[AccountTable.role],
                        isActive = row[AccountTable.isActive]
                    )

                    val gradeClass = row[NewGradeClassTable.id]?.value?.let { classId ->
                        GradeClassResponse(
                            id = classId,
                            name = row[NewGradeClassTable.name]
                        )
                    }

                    StudentProfileResponse(
                        id = row[StudentsTable.id].value,
                        user = user,
                        currentNewGradeClass = gradeClass,

                        isGraduated = row[StudentsTable.isGraduated],
                        lastSchoolAttended = row[StudentsTable.lastSchoolAttended],

                        isDiscountedStudent = row[StudentsTable.isDiscountedStudent],
                        isImmunized = row[StudentsTable.isImmunized],
                        hasAllergies = row[StudentsTable.hasAllergies],
                        hasPeculiarHealthIssues = row[StudentsTable.hasPeculiarHealthIssues],

                        allergicFoods = row[StudentsTable.allergicFoods],
                        healthIssues = row[StudentsTable.healthIssues],
                        otherRelatedInfo = row[StudentsTable.otherRelatedInfo],

                        nameOfFather = row[StudentsTable.nameOfFather],
                        nameOfMother = row[StudentsTable.nameOfMother],
                        occupationOfFather = row[StudentsTable.occupationOfFather],
                        occupationOfMother = row[StudentsTable.occupationOfMother],
                        nationalityOfFather = row[StudentsTable.nationalityOfFather],
                        nationalityOfMother = row[StudentsTable.nationalityOfMother],
                        contactOfFather = row[StudentsTable.contactOfFather],
                        contactOfMother = row[StudentsTable.contactOfMother],

                        houseNumber = row[StudentsTable.houseNumber]
                    )
                }
        }


    fun existsById(id: Int): Boolean = transaction {
            StudentsTable
                .selectAll()
                .where { StudentsTable.id eq id }
                .count() > 0
        }

    fun updateFull(id: Int, req: UpdateStudentRequest): Boolean = transaction {
            StudentsTable.update({ StudentsTable.id eq id }) { row ->

                row[StudentsTable.currentNewGradeClass] =
                    req.currentNewGradeClassId?.let { EntityID(it, NewGradeClassTable) }

                row[StudentsTable.isGraduated] = req.isGraduated
                row[StudentsTable.lastSchoolAttended] = req.lastSchoolAttended

                row[StudentsTable.isDiscountedStudent] = req.isDiscountedStudent
                row[StudentsTable.isImmunized] = req.isImmunized
                row[StudentsTable.hasAllergies] = req.hasAllergies
                row[StudentsTable.hasPeculiarHealthIssues] = req.hasPeculiarHealthIssues

                row[StudentsTable.allergicFoods] = req.allergicFoods
                row[StudentsTable.healthIssues] = req.healthIssues
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

    fun patch(id: Int, req: PatchStudentRequest): Boolean = transaction {
            StudentsTable.update({ StudentsTable.id eq id }) { row ->

                if (req.currentNewGradeClassId != null) {
                    row[StudentsTable.currentNewGradeClass] = EntityID(req.currentNewGradeClassId, NewGradeClassTable)
                }
                req.isGraduated?.let { row[StudentsTable.isGraduated] = it }
                if (req.lastSchoolAttended != null) row[StudentsTable.lastSchoolAttended] = req.lastSchoolAttended

                req.isDiscountedStudent?.let { row[StudentsTable.isDiscountedStudent] = it }
                req.isImmunized?.let { row[StudentsTable.isImmunized] = it }
                req.hasAllergies?.let { row[StudentsTable.hasAllergies] = it }
                req.hasPeculiarHealthIssues?.let { row[StudentsTable.hasPeculiarHealthIssues] = it }

                if (req.allergicFoods != null) row[StudentsTable.allergicFoods] = req.allergicFoods
                if (req.healthIssues != null) row[StudentsTable.healthIssues] = req.healthIssues
                if (req.otherRelatedInfo != null) row[StudentsTable.otherRelatedInfo] = req.otherRelatedInfo

                if (req.nameOfFather != null) row[StudentsTable.nameOfFather] = req.nameOfFather
                if (req.nameOfMother != null) row[StudentsTable.nameOfMother] = req.nameOfMother

                if (req.occupationOfFather != null) row[StudentsTable.occupationOfFather] = req.occupationOfFather
                if (req.occupationOfMother != null) row[StudentsTable.occupationOfMother] = req.occupationOfMother

                if (req.nationalityOfFather != null) row[StudentsTable.nationalityOfFather] = req.nationalityOfFather
                if (req.nationalityOfMother != null) row[StudentsTable.nationalityOfMother] = req.nationalityOfMother

                if (req.contactOfFather != null) row[StudentsTable.contactOfFather] = req.contactOfFather
                if (req.contactOfMother != null) row[StudentsTable.contactOfMother] = req.contactOfMother

                if (req.houseNumber != null) row[StudentsTable.houseNumber] = req.houseNumber
            } > 0
        }

    fun delete(id: Int): Boolean = transaction {
            StudentsTable.deleteWhere { StudentsTable.id eq id } > 0
        }


    fun patchNested(studentProfileId: Int, req: PatchStudentRequest): StudentProfileResponse? = transaction {

        // 1) Get the linked account id (because StudentsTable.user is a FK reference)
        val row = StudentsTable
            .selectAll()
            .where { StudentsTable.id eq studentProfileId }
            .singleOrNull()
            ?: return@transaction null

        val accountId = row[StudentsTable.user].value

        // 2) Update profile fields (only those provided)
        StudentsTable.update({ StudentsTable.id eq studentProfileId }) { u ->

            // FK (nullable) - only update if provided
            if (req.currentNewGradeClassId != null) {
                u[StudentsTable.currentNewGradeClass] = EntityID(req.currentNewGradeClassId, NewGradeClassTable)
            }

            req.isGraduated?.let { u[StudentsTable.isGraduated] = it }
            req.lastSchoolAttended?.let { u[StudentsTable.lastSchoolAttended] = it }

            req.isDiscountedStudent?.let { u[StudentsTable.isDiscountedStudent] = it }
            req.isImmunized?.let { u[StudentsTable.isImmunized] = it }
            req.hasAllergies?.let { u[StudentsTable.hasAllergies] = it }
            req.hasPeculiarHealthIssues?.let { u[StudentsTable.hasPeculiarHealthIssues] = it }

            req.allergicFoods?.let { u[StudentsTable.allergicFoods] = it }
            req.healthIssues?.let { u[StudentsTable.healthIssues] = it }
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

        // 3) Update nested account fields (only if user object exists)
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

        // 4) Return the updated nested response
        findByIdWithUserAndClass(studentProfileId)
    }


}




    fun findByIdWithUserAndClass(studentProfileId: Int): StudentProfileResponse? = transaction {

        val query = StudentsTable
            .join(
                otherTable = AccountTable,
                joinType = JoinType.INNER,
                onColumn = StudentsTable.user,
                otherColumn = AccountTable.id
            )
            .join(
                otherTable = NewGradeClassTable,
                joinType = JoinType.LEFT, // because class is nullable
                onColumn = StudentsTable.currentNewGradeClass,
                otherColumn = NewGradeClassTable.id
            )

        query
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
                    isActive = r[AccountTable.isActive]
                )

                val classDto = r[NewGradeClassTable.id]?.value?.let { classId ->
                    GradeClassResponse(
                        id = classId,
                        name = r[NewGradeClassTable.name]
                    )
                }

                StudentProfileResponse(
                    id = r[StudentsTable.id].value,
                    user = userDto,
                    currentNewGradeClass = classDto,

                    isGraduated = r[StudentsTable.isGraduated],
                    lastSchoolAttended = r[StudentsTable.lastSchoolAttended],

                    isDiscountedStudent = r[StudentsTable.isDiscountedStudent],
                    isImmunized = r[StudentsTable.isImmunized],
                    hasAllergies = r[StudentsTable.hasAllergies],
                    hasPeculiarHealthIssues = r[StudentsTable.hasPeculiarHealthIssues],

                    allergicFoods = r[StudentsTable.allergicFoods],
                    healthIssues = r[StudentsTable.healthIssues],
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







