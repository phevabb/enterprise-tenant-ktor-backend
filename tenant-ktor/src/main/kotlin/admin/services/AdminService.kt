package com.example.admin.services


import com.example.account.AccountRepository
import com.example.admin.dtos.requests.CreateAdminRequest
import com.example.admin.models.AdminProfile
import com.example.admin.repos.AdminRepository
import com.example.tenant.tenantTransaction
import kotlin.random.Random

object AdminService {

fun createAdmin(
    tenantSchema: String,
    request: CreateAdminRequest
) = tenantTransaction(tenantSchema) {
    println("===== CREATE ADMIN REQUEST RECEIVED =====")
    println("tenantSchema = $tenantSchema")
    println("fullName = ${request.user.fullName}")
    println("role = ${request.user.role}")

    // 1. Generate unique userId inside this tenant schema
    val generatedUserId = generateUniqueUserId(tenantSchema)
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

    // 4. Prevent duplicate admin profile inside current tenant transaction
    val existing = AdminRepository.findByUserIdInCurrentTransaction(user.id)

    if (existing != null) {
        println("Admin profile already exists for accountId=${user.id}")
        existing
    } else {
        println("Creating new admin profile for accountId=${user.id}")

        AdminRepository.createInCurrentTransaction(
            userId = user.id,
            telValue = request.tel

        )

//        AdminRepository.createInCurrentTransaction(
//            AdminProfile(
//                id = 0,
//                user = user.id,
//                tel = request.tel
//            )
//        )
    }
}

private fun generateUniqueUserId(tenantSchema: String): String {
    while (true) {
        val candidate = Random.Default.nextInt(10_000_000, 99_999_999).toString()

        val exists = tenantTransaction(tenantSchema) {
            AccountRepository.existsByUserIdInCurrentTransaction(candidate)
        }

        if (!exists) {
            return candidate
        }
    }
}

fun deleteAdmin(
    tenantSchema: String,
    id: Int
): Boolean = tenantTransaction(tenantSchema) {
    if (!AdminRepository.existsByIdInCurrentTransaction(id)) {
        println("Delete admin failed: admin profile not found for id=$id")
        return@tenantTransaction false
    }

    println("Deleting admin profile id=$id from tenantSchema=$tenantSchema")
    AdminRepository.deleteInCurrentTransaction(id)
}
}

