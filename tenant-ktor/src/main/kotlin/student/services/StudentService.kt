package com.example.student.services


import com.example.student.repos.StudentRepository


import com.example.tenant.tenantTransaction
import kotlin.random.Random
import com.example.student.dtos.requests.CreateStudentRequest
import com.example.student.dtos.requests.PatchStudentRequest
import com.example.account.AccountRepository
import com.example.student.dtos.requests.UpdateStudentRequest
import com.example.student.dtos.response.StudentProfileResponse
import com.example.student.models.StudentProfile

object StudentService {

    fun createStudent(
        tenantSchema: String,
        request: CreateStudentRequest
    ) = tenantTransaction(tenantSchema) {
        println("===== CREATE STUDENT REQUEST RECEIVED =====")
        println("tenantSchema = $tenantSchema")
        println("fullName = ${request.user.fullName}")
        println("role = ${request.user.role}")

        // 1. Generate unique userId inside current tenant schema
        val generatedUserId = generateUniqueUserIdInCurrentTransaction()
        println("Generated userId = $generatedUserId")

        // 2. Generate PIN / password
        val pin = Random.Default.nextInt(1000, 9999).toString()
        println("Generated PIN = $pin")

        // 3. Create account inside current tenant transaction
        val user = AccountRepository.createInCurrentTransaction(
            userId = generatedUserId,
            pin = pin,
            fullName = request.user.fullName,
            gender = request.user.gender,
            dateOfBirth = request.user.dateOfBirth,
            nationality = request.user.nationality,
            role = request.user.role.lowercase(),
            isActive = request.user.isActive,
            isStaff = request.user.isStaff
        )

        println("Account created successfully => accountId=${user.id}, userId=${user.userId}")

        // 4. Create StudentProfile inside current tenant transaction
        StudentRepository.createInCurrentTransaction(
            StudentProfile(
                id = 0,
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
                houseNumber = request.houseNumber
            )
        )
    }

    private fun generateUniqueUserIdInCurrentTransaction(): String {
        while (true) {
            val candidate = Random.Default.nextInt(10_000_000, 99_999_999).toString()

            if (!AccountRepository.existsByUserIdInCurrentTransaction(candidate)) {
                return candidate
            }
        }
    }

    fun updateStudent(
        tenantSchema: String,
        id: Int,
        req: UpdateStudentRequest
    ): Boolean = tenantTransaction(tenantSchema) {
        if (!StudentRepository.existsByIdInCurrentTransaction(id)) {
            println("Update student failed: student profile not found for id=$id")
            return@tenantTransaction false
        }

        StudentRepository.updateFullInCurrentTransaction(id, req)
    }

    fun patchStudent(
        tenantSchema: String,
        id: Int,
        req: PatchStudentRequest
    ): Boolean = tenantTransaction(tenantSchema) {
        if (!StudentRepository.existsByIdInCurrentTransaction(id)) {
            println("Patch student failed: student profile not found for id=$id")
            return@tenantTransaction false
        }

        StudentRepository.patchInCurrentTransaction(id, req)
    }

    fun deleteStudent(
        tenantSchema: String,
        id: Int
    ): Boolean = tenantTransaction(tenantSchema) {
        if (!StudentRepository.existsByIdInCurrentTransaction(id)) {
            println("Delete student failed: student profile not found for id=$id")
            return@tenantTransaction false
        }

        StudentRepository.deleteInCurrentTransaction(id)
    }

    fun patchStudentNested(
        tenantSchema: String,
        id: Int,
        req: PatchStudentRequest
    ): StudentProfileResponse? = tenantTransaction(tenantSchema) {
        StudentRepository.patchNestedInCurrentTransaction(id, req)
    }
}

