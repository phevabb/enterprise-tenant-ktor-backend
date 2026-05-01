package com.example.student.mappers

import com.example.student.StudentsTable
import com.example.student.models.StudentProfile
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toStudentProfile() = StudentProfile(
    id = this[StudentsTable.id].value,
    user = this[StudentsTable.user].value,
    family = this[StudentsTable.family]?.value,

    currentNewGradeClassId = this[StudentsTable.currentNewGradeClass]?.value,

    isGraduated = this[StudentsTable.isGraduated],
    lastSchoolAttended = this[StudentsTable.lastSchoolAttended],

    isDiscountedStudent = this[StudentsTable.isDiscountedStudent],
    isImmunized = this[StudentsTable.isImmunized],
    hasAllergies = this[StudentsTable.hasAllergies],


    allergicFoods = this[StudentsTable.allergicFoods],

    otherRelatedInfo = this[StudentsTable.otherRelatedInfo],

    nameOfFather = this[StudentsTable.nameOfFather],
    nameOfMother = this[StudentsTable.nameOfMother],
    occupationOfFather = this[StudentsTable.occupationOfFather],
    occupationOfMother = this[StudentsTable.occupationOfMother],
    nationalityOfFather = this[StudentsTable.nationalityOfFather],
    nationalityOfMother = this[StudentsTable.nationalityOfMother],
    contactOfFather = this[StudentsTable.contactOfFather],
    contactOfMother = this[StudentsTable.contactOfMother],

    houseNumber = this[StudentsTable.houseNumber]
)




