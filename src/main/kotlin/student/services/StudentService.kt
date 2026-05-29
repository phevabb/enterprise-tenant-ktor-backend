package com.example.student.services

import com.example.account.AccountRepository
import com.example.student.dtos.requests.CreateStudentRequest
import com.example.student.dtos.requests.PatchStudentRequest
import com.example.student.dtos.requests.UpdateStudentRequest
import com.example.student.dtos.response.StudentProfileResponse
import com.example.student.models.StudentProfile
import com.example.student.repos.StudentRepository
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.random.Random

object StudentService {

    fun createStudent(request: CreateStudentRequest) = transaction {

        // 1️⃣ Generate unique user_id (same logic as Django)
        val userId = generateUniqueUserId()

        // 2️⃣ Generate PIN / password
        val pin = Random.Default.nextInt(1000, 9999).toString()

        // 3️⃣ Create User
        val user = AccountRepository.create(
            userId = userId,
            pin = pin,
            fullName = request.user.fullName,
            gender = request.user.gender,
            dateOfBirth = request.user.dateOfBirth,
            nationality = request.user.nationality,
            role = request.user.role.lowercase(),
            isActive = request.user.isActive,
            isStaff = request.user.isStaff
        )

        // 4️⃣ Create StudentProfile explicitly
        StudentRepository.create(
            StudentProfile(
                id = 0, // ignored, DB generates it
                user = user.id,
                currentNewGradeClassId = request.currentNewGradeClassId,
                family = request.family?.takeIf { it > 0 },
                isGraduated = false,
                lastSchoolAttended = request.lastSchoolAttended,
                isDiscountedStudent = request.isDiscountedStudent,
                isImmunized = request.isImmunized,
                hasAllergies = request.hasAllergies,

                allergicFoods = request.allergicFoods,

                otherRelatedInfo = request.otherRelatedInfo,
                nameOfFather = request.nameOfFather,
                nameOfMother = request.nameOfMother,
                occupationOfFather = request.occupationOfFather,
                occupationOfMother = request.occupationOfMother,
                nationalityOfFather = request.nationalityOfFather,
                nationalityOfMother = request.nationalityOfMother,
                contactOfFather = request.contactOfFather,
                contactOfMother = request.contactOfMother,
                houseNumber = request.houseNumber,
            )
        )
    }



    private fun generateUniqueUserId(): String {
        while (true) {
            val candidate = Random.Default.nextInt(10_000_000, 99_999_999).toString()
            if (!AccountRepository.existsByUserId(candidate)) {
                return candidate
            }
        }
    }



    fun updateStudent(id: Int, req: UpdateStudentRequest): Boolean {
            if (!StudentRepository.existsById(id)) return false
            return StudentRepository.updateFull(id, req)
        }

    fun patchStudent(id: Int, req: PatchStudentRequest): Boolean {
            if (!StudentRepository.existsById(id)) return false
            return StudentRepository.patch(id, req)
        }

    fun deleteStudent(id: Int): Boolean {
            if (!StudentRepository.existsById(id)) return false
            return StudentRepository.delete(id)
        }



    fun patchStudentNested(id: Int, req: PatchStudentRequest): StudentProfileResponse? {
        return StudentRepository.patchNested(id, req)
    }




}