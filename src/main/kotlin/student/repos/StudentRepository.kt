package com.example.student.repos


import com.example.student.StudentsTable

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


import com.example.account.AccountTable
import com.example.familyfees.tables.FamilyTable
import com.example.minimals.FamilyMinimal
import com.example.staff.dtos.response.StudentLiteResponse
import com.example.student.dtos.requests.PatchStudentRequest
import com.example.student.dtos.response.PerClassResponse


import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update


object StudentRepository {

    /** ✅ Create student profile */
    fun create(profile: StudentProfile) = transaction {

        val id = StudentsTable.insertAndGetId {

            // ✅ FK → EntityID<Int>
            it[user] = EntityID(profile.user, AccountTable)

            // ✅ Nullable FK
            it[currentNewGradeClass] =
                profile.currentNewGradeClassId?.let {
                    EntityID(it, NewGradeClassTable)
                }

            it[family] =
                profile.family?.let {
                    EntityID(it, FamilyTable)
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

        findByIdWithUserAndClass(id)!!
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

    fun findAllWithUserAndClassRaw(search: String?): List<StudentProfileResponse> = transaction {

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

        query
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

                val fam = row[FamilyTable.id]?.value?.let {
                    FamilyMinimal(it, row[FamilyTable.name])
                }

                StudentProfileResponse(
                    id = row[StudentsTable.id].value,
                    user = user,
                    currentNewGradeClass = gradeClass,
                    family = fam,
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
                    hasAllergies = row[StudentsTable.hasAllergies],
                )
            }
    }


    fun countStudents(): Int = transaction {
        StudentsTable.selectAll().count().toInt()
    }

    fun findAllWithUserAndClass(
        page: Int,
        limit: Int,
        search: String?
    ): Pair<List<StudentProfileResponse>, Long> = transaction {

        val offset = ((page - 1) * limit).toLong()

        // ✅ base query
        val query = StudentsTable
            .join(AccountTable, JoinType.INNER, StudentsTable.user, AccountTable.id)
            .join(NewGradeClassTable, JoinType.LEFT, StudentsTable.currentNewGradeClass, NewGradeClassTable.id)
            .join(FamilyTable, JoinType.LEFT, StudentsTable.family, FamilyTable.id)
            .selectAll()

        // ✅ APPLY SEARCH
        if (!search.isNullOrBlank()) {
            val q = "%${search.lowercase()}%"

            query.andWhere {
                (AccountTable.fullName.lowerCase() like q) or
                        (NewGradeClassTable.name.lowerCase() like q) or
                        (StudentsTable.contactOfFather.lowerCase() like q) or
                        (StudentsTable.contactOfMother.lowerCase() like q)
            }
        }

        // ✅ COUNT AFTER FILTER
        val total = query.count()

        // ✅ FETCH DATA
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

                val fam = row[FamilyTable.id]?.value?.let {
                    FamilyMinimal(it, row[FamilyTable.name])
                }

                StudentProfileResponse(
                    id = row[StudentsTable.id].value,
                    user = user,
                    currentNewGradeClass = gradeClass,
                    family = fam,
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
                    hasAllergies = row[StudentsTable.hasAllergies],
                )
            }

        Pair(data, total)
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




        fun patch(id: Int, req: PatchStudentRequest): Boolean = transaction {

            // 1) Get linked account id (needed for nested user update)
            val existing = StudentsTable
                .selectAll()
                .where { StudentsTable.id eq id }
                .singleOrNull()
                ?: return@transaction false

            val accountId = existing[StudentsTable.user].value

            // 2) Update StudentProfile fields
            val updated = StudentsTable.update({ StudentsTable.id eq id }) { row ->

                // ✅ FK update:
                // If frontend always sends currentNewGradeClassId (null or value), this is correct:
                // - value -> set FK
                // - null  -> clear FK
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

                // ✅ Only include these if the columns exist in StudentsTable:
                // req.classSeekingAdmissionTo?.let { row[StudentsTable.classSeekingAdmissionTo] = it }
                // req.deactivationReason?.let { row[StudentsTable.deactivationReason] = it }
            }

            if (updated == 0) return@transaction false

            // 3) Update nested Account fields (if user object exists)
            req.user?.let { u ->
                AccountTable.update({ AccountTable.id eq accountId }) { a ->
                    u.fullName?.let { a[AccountTable.fullName] = it }
                    u.gender?.let { a[AccountTable.gender] = it }
                    u.nationality?.let { a[AccountTable.nationality] = it }
                    u.dateOfBirth?.let { a[AccountTable.dateOfBirth] = it }
                    u.role?.let { a[AccountTable.role] = it.lowercase() }
                    u.isActive?.let { a[AccountTable.isActive] = it }
                    u.isStaff?.let { a[AccountTable.isStaff] = it }
                    // u.id is intentionally ignored
                }
            }

            true
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


    fun findStudentsByClass(classId: Int): List<StudentLiteResponse> = transaction {

        StudentsTable
            .join(
                AccountTable,
                JoinType.INNER,
                StudentsTable.user,
                AccountTable.id
            )
            .selectAll()
            .where {
                StudentsTable.currentNewGradeClass eq classId
            }
            .orderBy(AccountTable.fullName to SortOrder.ASC)
            .map { row ->

                StudentLiteResponse(
                    id = row[StudentsTable.id].value,
                    full_name = row[AccountTable.fullName],
                    indexNo = row[AccountTable.userId]
                )
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
            .join(FamilyTable, JoinType.LEFT, onColumn = StudentsTable.family, otherColumn = FamilyTable.id)


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
                    isActive = r[AccountTable.isActive],
                    pin = r[AccountTable.pin],
                    dateOfBirth = r[AccountTable.dateOfBirth ],
                    profilePictureUrl = r[AccountTable.profilePictureUrl],
                    profilePicturePublicId = r[AccountTable.profilePicturePublicId]

                )

                val classDto = r[NewGradeClassTable.id]?.value?.let { classId ->
                    GradeClassResponse(
                        id = classId,
                        name = r[NewGradeClassTable.name]
                    )
                }

                val famdto = r[FamilyTable.id]?.value?.let { famId ->
                    FamilyMinimal(
                        id = famId,
                        name = r[FamilyTable.name]
                    )
                }

                StudentProfileResponse(
                    id = r[StudentsTable.id].value,
                    user = userDto,
                    currentNewGradeClass = classDto,
                    family = famdto,
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

    fun countPerClass(): List<PerClassResponse> = transaction {

        val countExpr = StudentsTable.id.count()

        StudentsTable
            .leftJoin(
                NewGradeClassTable,
                { StudentsTable.currentNewGradeClass },
                { NewGradeClassTable.id }
            )
            .select(NewGradeClassTable.name, countExpr)
            .groupBy(NewGradeClassTable.name)
            .orderBy(NewGradeClassTable.name to SortOrder.ASC)
            .map { row ->   // ✅ THIS is where it goes

                PerClassResponse(
                    `class` = row[NewGradeClassTable.name] ?: "No Class Assigned",
                    count = row[countExpr]
                )
            }
    }





}
















