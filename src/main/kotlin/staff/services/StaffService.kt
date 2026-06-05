package com.example.staff.services

import com.example.account.AccountRepository
import com.example.staff.dtos.requests.CreateStaffRequest
import com.example.staff.models.StaffProfile
import com.example.staff.repos.StaffRepository
import com.example.tenant.tenantTransaction
import kotlin.random.Random


object StaffService {

    fun createStaff(
        tenantSchema: String,
        request: CreateStaffRequest
    ) = tenantTransaction(tenantSchema) {
        println("===== CREATE STAFF REQUEST RECEIVED =====")
        println("tenantSchema = $tenantSchema")
        println("fullName = ${request.user.fullName}")
        println("role = ${request.user.role}")

        // 1. Generate unique userId inside current tenant schema
        val generatedUserId = generateUniqueUserIdInCurrentTransaction()
        println("Generated userId = $generatedUserId")

        // 2. Generate PIN
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

        // 4. Prevent duplicate staff profile inside current tenant transaction
        val existing = StaffRepository.findByUserIdInCurrentTransaction(user.id)

        if (existing != null) {
            println("Staff profile already exists for accountId=${user.id}")
            existing
        } else {
            println("Creating new staff profile for accountId=${user.id}")

            StaffRepository.createInCurrentTransaction(
                StaffProfile(
                    id = 0,
                    user = user.id,
                    assignedClassId = request.assignedClassId,
                    tel = request.tel
                )
            )
        }
    }

    private fun generateUniqueUserIdInCurrentTransaction(): String {
        while (true) {
            val candidate = Random.Default.nextInt(10_000_000, 99_999_999).toString()

            if (!AccountRepository.existsByUserIdInCurrentTransaction(candidate)) {
                return candidate
            }
        }
    }

    fun deleteStaff(
        tenantSchema: String,
        id: Int
    ): Boolean = tenantTransaction(tenantSchema) {
        if (!StaffRepository.existsByIdInCurrentTransaction(id)) {
            println("Delete staff failed: staff profile not found for id=$id")
            return@tenantTransaction false
        }

        println("Deleting staff profile id=$id from tenantSchema=$tenantSchema")
        StaffRepository.deleteInCurrentTransaction(id)
    }
}
