package com.example.student.services


import com.example.student.repos.StudentRepository


import com.example.tenant.tenantTransaction

import tenant.findDefaultDomainBySchema

import kotlin.random.Random
import com.example.student.dtos.requests.CreateStudentRequest
import com.example.student.dtos.requests.PatchStudentRequest
import com.example.account.AccountRepository
import com.example.student.dtos.requests.UpdateStudentRequest
import com.example.student.dtos.response.StudentProfileResponse
import com.example.student.models.StudentProfile
import com.example.student.tables.NewGradeClassTable
import student.services.AdmissionSmsNotifier
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object StudentService {

    private fun buildAdmissionSmsMessage(
        customMessage: String,
        details: CreatedStudentAdmissionDetails,
        schoolUrl: String
    ): String {

        val normalizedSchoolUrl =
            schoolUrl
                .trim()
                .let { domain ->

                    when {

                        domain.isBlank() -> {
                            "Not available"
                        }

                        domain.startsWith(
                            "http://",
                            ignoreCase = true
                        ) -> {
                            domain
                        }

                        domain.startsWith(
                            "https://",
                            ignoreCase = true
                        ) -> {
                            domain
                        }

                        else -> {
                            "https://$domain"
                        }
                    }
                }

        val formattedDateOfBirth =
            details.dateOfBirth
                ?.trim()
                ?.takeIf { dateOfBirth ->
                    dateOfBirth.isNotBlank()
                }
                ?: "Not provided"

        val fatherContact =
            details.contactOfFather
                .trim()
                .takeIf { contact ->
                    contact.isNotBlank()
                }
                ?: "Not provided"

        val motherContact =
            details.contactOfMother
                ?.trim()
                ?.takeIf { contact ->
                    contact.isNotBlank()
                }
                ?: "Not provided"

        return buildString {

            if (customMessage.isNotBlank()) {
                appendLine(
                    customMessage.trim()
                )

                appendLine()
            }

            appendLine(
                "Student: ${details.studentName.uppercase()}"
            )

            appendLine(
                "User ID: ${details.userId}"
            )

            appendLine(
                "PIN: ${details.pin}"
            )

            appendLine(
                "Admission date: ${details.dateOfAdmission}"
            )

            appendLine(
                "Date of birth: $formattedDateOfBirth"
            )

            appendLine(
                "Class: ${details.className}"
            )

            appendLine(
                "Father's phone: $fatherContact"
            )

            appendLine(
                "Mother's phone: $motherContact"
            )

            appendLine(
                "Login URL: $normalizedSchoolUrl"
            )

            append(
                "Log in to view academic performance and download report cards."
            )
        }
            .trim()
    }


            private data class CreatedStudentAdmissionDetails(
        val profile: StudentProfileResponse,
        val studentName: String,
        val userId: String,
        val pin: String,
        val dateOfAdmission: String,
        val dateOfBirth: String?,
        val className: String,
        val contactOfFather: String,
        val contactOfMother: String?
    )



    fun createStudent(
        tenantSchema: String,
        request: CreateStudentRequest
    ): StudentProfileResponse {

        if (request.sendAdmissionSms) {
            require(
                !request.admissionSmsMessage.isNullOrBlank()
            ) {
                "Admission SMS message is required when admission SMS is enabled."
            }
        }

        val createdStudentDetails =
            tenantTransaction(
                tenantSchema
            ) {
                println(
                    "===== CREATE STUDENT REQUEST RECEIVED ====="
                )

                println(
                    "tenantSchema = $tenantSchema"
                )

                println(
                    "fullName = ${request.user.fullName}"
                )

                println(
                    "role = ${request.user.role}"
                )

                println(
                    "sendAdmissionSms = ${request.sendAdmissionSms}"
                )

                val generatedUserId =
                    generateUniqueUserIdInCurrentTransaction()

                val pin =
                    Random.Default
                        .nextInt(
                            1000,
                            10000
                        )
                        .toString()

                val user =
                    AccountRepository
                        .createInCurrentTransaction(
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

                val createdProfile =
                    StudentRepository
                        .createInCurrentTransaction(
                            StudentProfile(
                                id = 0,
                                user = user.id,
                                currentNewGradeClassId =
                                    request.currentNewGradeClassId,

                                family =
                                    request.family
                                        ?.takeIf {
                                            it > 0
                                        },

                                isGraduated = false,

                                lastSchoolAttended =
                                    request.lastSchoolAttended,

                                isDiscountedStudent =
                                    request.isDiscountedStudent,

                                isImmunized =
                                    request.isImmunized,

                                hasAllergies =
                                    request.hasAllergies,

                                allergicFoods =
                                    request.allergicFoods,

                                otherRelatedInfo =
                                    request.otherRelatedInfo,

                                nameOfFather =
                                    request.nameOfFather,

                                nameOfMother =
                                    request.nameOfMother,

                                occupationOfFather =
                                    request.occupationOfFather,

                                occupationOfMother =
                                    request.occupationOfMother,

                                nationalityOfFather =
                                    request.nationalityOfFather,

                                nationalityOfMother =
                                    request.nationalityOfMother,

                                contactOfFather =
                                    request.contactOfFather,

                                contactOfMother =
                                    request.contactOfMother,

                                houseNumber =
                                    request.houseNumber
                            )
                        )

                val className =
                    request.currentNewGradeClassId
                        ?.let { classId ->
                            NewGradeClassTable
                                .select(
                                    NewGradeClassTable.name
                                )
                                .where {
                                    NewGradeClassTable.id eq classId
                                }
                                .limit(1)
                                .singleOrNull()
                                ?.get(
                                    NewGradeClassTable.name
                                )
                        }
                        ?: "Not assigned"

                CreatedStudentAdmissionDetails(
                    profile = createdProfile,
                    studentName = request.user.fullName.trim(),
                    userId = generatedUserId,
                    pin = pin,
                    dateOfAdmission =
                        LocalDate.now().format(
                            DateTimeFormatter.ofPattern(
                                "dd MMM yyyy"
                            )
                        ),
                    dateOfBirth =
                        request.user.dateOfBirth
                            ?.toString(),
                    className = className,
                    contactOfFather =
                        request.contactOfFather,
                    contactOfMother =
                        request.contactOfMother
                )
            }

        if (request.sendAdmissionSms) {
            val defaultDomain =
                findDefaultDomainBySchema(
                    tenantSchema = tenantSchema
                )

            val customMessage =
                request.admissionSmsMessage
                    ?.trim()
                    .orEmpty()

            val admissionMessage =
                buildAdmissionSmsMessage(
                    customMessage = customMessage,
                    details = createdStudentDetails,
                    schoolUrl = defaultDomain
                )

            AdmissionSmsNotifier.notifyAsync(
                tenantSchema = tenantSchema,
                studentName = createdStudentDetails.studentName,
                contactOfFather = createdStudentDetails.contactOfFather,

                message = admissionMessage
            )
        }

        return createdStudentDetails.profile
    }



//    fun createStudent(
//        tenantSchema: String,
//        request: CreateStudentRequest
//    ) = tenantTransaction(tenantSchema) {
//        println("===== CREATE STUDENT REQUEST RECEIVED =====")
//        println("tenantSchema = $tenantSchema")
//        println("fullName = ${request.user.fullName}")
//        println("role = ${request.user.role}")
//
//        // 1. Generate unique userId inside current tenant schema
//        val generatedUserId = generateUniqueUserIdInCurrentTransaction()
//        println("Generated userId = $generatedUserId")
//
//        // 2. Generate PIN / password
//        val pin = Random.Default.nextInt(1000, 9999).toString()
//        println("Generated PIN = $pin")
//
//        // 3. Create account inside current tenant transaction
//        val user = AccountRepository.createInCurrentTransaction(
//            userId = generatedUserId,
//            pin = pin,
//            fullName = request.user.fullName,
//            gender = request.user.gender,
//            dateOfBirth = request.user.dateOfBirth,
//            nationality = request.user.nationality,
//            role = request.user.role.lowercase(),
//            isActive = request.user.isActive,
//            isStaff = request.user.isStaff
//        )
//
//        println("Account created successfully => accountId=${user.id}, userId=${user.userId}")
//
//        // 4. Create StudentProfile inside current tenant transaction
//        StudentRepository.createInCurrentTransaction(
//            StudentProfile(
//                id = 0,
//                user = user.id,
//                currentNewGradeClassId = request.currentNewGradeClassId,
//                family = request.family?.takeIf { it > 0 },
//                isGraduated = false,
//                lastSchoolAttended = request.lastSchoolAttended,
//                isDiscountedStudent = request.isDiscountedStudent,
//                isImmunized = request.isImmunized,
//                hasAllergies = request.hasAllergies,
//                allergicFoods = request.allergicFoods,
//                otherRelatedInfo = request.otherRelatedInfo,
//                nameOfFather = request.nameOfFather,
//                nameOfMother = request.nameOfMother,
//                occupationOfFather = request.occupationOfFather,
//                occupationOfMother = request.occupationOfMother,
//                nationalityOfFather = request.nationalityOfFather,
//                nationalityOfMother = request.nationalityOfMother,
//                contactOfFather = request.contactOfFather,
//                contactOfMother = request.contactOfMother,
//                houseNumber = request.houseNumber
//            )
//        )
//    }










































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

